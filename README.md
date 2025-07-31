# Analysis of compressed rest to gRPC

## Mission statement
This project is used to analyze the network traffic of rest, compressed rest and gRPC. First to compare the 
plain text rest with gRPC and then to test what is needed to use compressed rest and compare it with gRPC.

## Project setup
The project has a client and a server. The client sends the rest/gRPC requests to the server and the server
responds to the client. The client and server are implemented in Kotlin with Spring Boot and are both in a
Maven multimodule project. The server is a normal Spring Boot Rest/gRPC application that can handle the requests.
The client is also a Spring Boot application that sends the requests to the server after the startup has finished.
Spring Boot provides support for compressed rest responses and gRPC responses with starters in the spring initilzr.

## How to run the project
1. Clone the repository
2. Import the project into your IDE
3. Import the Maven dependencies
4. Start the server with the DemoApplication class
5. Start the client with the DemoclientApplication class
6. Inspect the logs of the server and the client

## Rest and gRPC Protocols
The Rest protocol is the standard to communicate with endpoints to request and send data. It is human readable
and has very good support in Spring Boot. It has large body sizes due the good readability, because of this 
Spring Boot supports the compression of the responses out of the box with these values in the application.properties file:
```properties
server.compression.enabled=true
server.compression.min-response-size=2048
server.compression.mime-types=text/html,text/xml,text/plain,text/css,application/json
```
The gRPC protocol is a binary protocol that is used to communicate with endpoints. It is not human readable
and has a smaller body size than the Rest protocol. It needs a proto file to define the dtos and the service endpoints.
The contents of the proto file is compiled during the build to Java classes that can be used to implement the service endpoints.
The proto file can be found in the `src/main/proto` directory.

## Implementation of the gRPC server
To add the gRPC server to the project, you need to add these gRPC dependencies to the server's pom.xml file:
```xml
<dependency>
	<groupId>org.springframework.grpc</groupId>
	<artifactId>spring-grpc-server-web-spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>io.grpc</groupId>
	<artifactId>grpc-services</artifactId>
</dependency>
```
The Spring Initializr provides a starter for gRPC. To create the gRPC endpoint the class GrpcDemoController is used.  
```kotlin
@GrpcService
class GrpcDemoController(val responseService: ResponseService): LargeResponseProviderGrpc.LargeResponseProviderImplBase() {
    override fun getLargeResponse(request: Empty?, responseObserver: StreamObserver<LargeResponse?>?) {
        val response = LargeResponse.newBuilder()
            .addAllKeyValuePairs(this.responseService.createLargeResponse().keyValuePairs.map { pair ->
                KeyValuePair.newBuilder().setKey(pair.key).setValue(pair.value).build()
            })
            .build()
        responseObserver?.onNext(response)
        responseObserver?.onCompleted()
    }
}
```
The `GrpcDemoController` class is annotated with `@GrpcService` to indicate that it is a gRPC service. The `getLargeResponse` method is the endpoint that will be called by the client. 
It uses the `LargeResponseProviderGrpc.LargeResponseProviderImplBase` class that was generated based on the `demo.proto` file to implement the endpoint. 
The response is built using the `LargeResponse` and `KeyValuePair` classes that are generated from the proto file.
The `KeyValuePair` is filled with values from the `ResponseService` class, which is responsible for creating the content of the LargeResponse.
The `responseObserver?.onNext(...)` method is used to set the value of the gRPC response. The `onComplete()` method is called to send the response.

## Implementation of the Rest server
The setup of the Rest endpoints in Spring Boot does not need to be explained again. With theses properties Spring Boot supports the 
response compression out of the box:
```properties
server.compression.enabled=true
server.compression.min-response-size=2048
server.compression.mime-types=text/html,text/xml,text/plain,text/css,application/json
```
The mime types in the list are the ones that are compressed if the have a minimum size of 2048 bytes. To handle 
the rest requests the `DemonoController` class is used.

To support the compression of the request bodies the `DecompressionFilter` class is used. 
It enables the use of compressed request bodies in the rest endpoints. That makes the compression of the requests and responses possible:
```kotlin
@Component
class DecompressionFilter: Filter {

    override fun doFilter(
        request: ServletRequest?,
        response: ServletResponse?,
        filterChain: FilterChain?
    ) {
        val req: HttpServletRequest = request as HttpServletRequest
        filterChain?.doFilter(DecompressionWrapper(req, req.getHeader(HttpHeaders.CONTENT_ENCODING) ?: ""), response)
    }
}

class DecompressionWrapper(request: HttpServletRequest, val contentEncoding: String): HttpServletRequestWrapper(request) {
    enum class ContentEncoding(val value: String) {
        GZIP("gzip"), DEFLATE("deflate")
    }
    val sourceStream = if(contentEncoding == ContentEncoding.GZIP.value)  GZIPInputStream(request.inputStream) else if(contentEncoding == ContentEncoding.DEFLATE.value) DeflaterInputStream(request.inputStream) else request.inputStream
    private var finished = false

    override fun getInputStream(): ServletInputStream {
        val decompressionObject =  object : ServletInputStream() {

            @Throws(IOException::class)
            override fun read(): Int {
                val data: Int = sourceStream.read()
                if (data == -1) {
                    finished = true
                }

                return data
            }

            @Throws(IOException::class)
            override fun available(): Int {
                return sourceStream.available()
            }

            @Throws(IOException::class)
            override fun close() {
                super.close()
                sourceStream.close()
            }

            override fun isFinished(): Boolean {
                return finished
            }

            override fun isReady(): Boolean {
                return true
            }

            override fun setReadListener(readListener: ReadListener) {
                throw UnsupportedOperationException()
            }
        }
        return if( contentEncoding == ContentEncoding.GZIP.value || contentEncoding == ContentEncoding.DEFLATE.value) {
            decompressionObject
        } else {
            request.inputStream
        }
    }
}
```
The `DecompressionFilter` class is a filter that is used in the Spring Boot filter chain for tomcat. All requests and responses pass through this filter in the chain. 
The `DecompressionFilter` uses the `DecompressionWrapper` class to handle the request input stream according to the Content-Encoding header value.
The `DecompressionWrapper` class checks the Content-Encoding header and uses the appropriate decompression method (GZIP or DEFLATE) to wrap the request input stream.
The `getInputStream()` method creates and returns a `ServletInputStream` object with the wrapped input stream based on the Content-Encoding header value or returns the original ServletInputStream.

## The service to generate the large response
The `ResponseService` class is used to generate the large response dtos that are used to create the responses for gRPC and Rest.
```kotlin
@Service
class ResponseService {
    val logger = LoggerFactory.getLogger(ResponseService::class.java)

    fun createLargeResponse(largeResponse: LargeResponse?): LargeResponse {
        logger.info("Received response with ${largeResponse?.keyValuePairs?.size} key-value pairs.")
        return this.createLargeResponse()
    }

    fun createLargeResponse(): LargeResponse {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val keyValuePairs = (1..100000).map { index ->
            val key = (1..20)
                .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
                .joinToString("")
            KeyValuePair(key, Random.nextLong(0, 1000000000000000))
        }
        return LargeResponse(keyValuePairs)
    }

    @Bean
    fun customizer(): TomcatConnectorCustomizer {
        return TomcatConnectorCustomizer { connector: Connector? ->
            for (protocol in connector!!.findUpgradeProtocols()) {
                if (protocol is Http2Protocol) {
                    protocol.setOverheadWindowUpdateThreshold(0)
                }
            }
        }
    }
}
```
The method `createLargeResponse()` generates a large response with 100,000 key-value pairs. The keys are random strings of 20 characters and the values are random long numbers.
The `customizer()` method is used to set the overhead window update threshold for the HTTP/2 protocol to 0. To ensure that there is no overhead in the response size.

