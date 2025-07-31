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

The `demo.proto` file defines the gRPC service and the messages used in the service:
```proto
syntax = "proto3";

package demoDto;

option java_multiple_files = true;
option java_package = "com.example.democlient.rpc";

service LargeResponseProvider {
  rpc getLargeResponse(Empty) returns (LargeResponse) {}
}

message LargeResponse {
  repeated KeyValuePair keyValuePairs=1;
}

message KeyValuePair {
  string key=1;
  int64 value=2;
}

message Empty {}
```
The options `java_multiple_files` and `java_package` are used to generate the Java classes in the specified package. 
The `LargeResponseProvider` service defines the `getLargeResponse` rpc method that takes an `Empty` message as input and returns a `LargeResponse` message.
The `LargeResponse` message contains a repeated field of `KeyValuePair` messages, which are used to store the key-value pairs in the response.
The `KeyValuePair` message contains a string key and a long value. 
The `Empty` message is used as a placeholder for the input parameter of the `getLargeResponse` method.

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

## The implementation of the gRPC client
The client is implemented in the `DemoGrpcClient` class: 
```kotlin
@Service
class DemoGrpcClient(val provider: LargeResponseProviderGrpc.LargeResponseProviderBlockingStub) {
    val logger = LoggerFactory.getLogger(DemoGrpcClient::class.java)

    @EventListener
    fun handleEvent(event: ApplicationReadyEvent) {
        val response = this.getLargeResponse()
        logger.info("Response content length: ${response.serializedSize} bytes")
        logger.info("Received response with ${response.keyValuePairsList.size} key-value pairs.")
    }

    fun getLargeResponse(): LargeResponse {
        return this.provider.getLargeResponse(Empty.newBuilder().build())
    }
}
```
The `DemoGrpcClient` class gets the `LargeResponseProviderBlockingStub` injected as `provider` that is created during the build based on the `demo.proto` file.
The method `getLargeResponse()` uses the provider to call the gRPC endpoint and return the `LargeResponse` object. The empty object is as placeholder needed for 
a rpc method with a parameter. 
The `handleEvent()` method is annotated with `@EventListener` to listen for the `ApplicationReadyEvent` event. This method is called after the application has started and calls the `getLargeResponse()` method to request a large response from the server.

The `LargeResponseProviderBlockingStub` is a blocking stub that is used to call the gRPC endpoint is created in the `DemoGrpcConfig` class:
```kotlin
@Configuration
class DemoGrpcConfig {
    @Bean
    fun stub(channels: GrpcChannelFactory): LargeResponseProviderGrpc.LargeResponseProviderBlockingStub? {
        return LargeResponseProviderGrpc.newBlockingStub(channels.createChannel("large-response"))
    }
}
```
With the `@Bean` annotation the `stub()` method is used to create the `LargeResponseProviderBlockingStub` bean that is then available for injection in the `DemoGrpcClient` class.
To enable the creation of the `LargeResponseProviderBlockingStub` bean the `large-response` channel has to be configured in the `application.properties` file:
```properties
spring.grpc.client.channels.large-response.address=localhost:8080
spring.grpc.client.channels.large-response.negotiation-type=plaintext
```
This configuration sets the address of the gRPC server to `localhost:8080` and uses plaintext negotiation for the gRPC communication. An alternative would be encryption with TLS, but this is not used in this project.

## The implementation of the Rest client
The client is implemented in the `DemoRestClient` class:
```kotlin
@Service
class DemoRestClient {
    val logger = LoggerFactory.getLogger(DemoRestClient::class.java)
    val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    val restClient = RestClient.create();

    @EventListener
    fun handleEvent(event: ApplicationReadyEvent) {
        val response = this.getLargeResponse()
        logger.info("Received get response with ${response?.keyValuePairs?.size} key-value pairs.")
        val responseCompressed = this.getLargeResponseCompressed()
        logger.info("Received get compressed response with ${responseCompressed?.keyValuePairs?.size} key-value pairs.")
        val postResponse = this.postLargeResponseCompressed()
        logger.info("Received post compresssed request/response with ${postResponse?.keyValuePairs?.size} key-value pairs.")
    }

    fun getLargeResponse(): LargeResponse? {
        val response = restClient.get().uri("http://localhost:8080/rest/demo/large-response")
            .header(HttpHeaders.ACCEPT_ENCODING, "identity")
            .exchange { request, response ->
                //logger.info(response.headers.get(HttpHeaders.CONTENT_ENCODING)?.get(0)?.let { "Content-Encoding: $it" } ?: "No Content-Encoding header found")
                handleEncodedResponse(response)
            }
        return response
    }

    fun getLargeResponseCompressed(): LargeResponse? {
        val response = restClient.get().uri("http://localhost:8080/rest/demo/large-response")
            .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
            .exchange { request, response ->
                    //logger.info(response.headers.get(HttpHeaders.CONTENT_ENCODING)?.get(0)?.let { "Content-Encoding: $it" } ?: "No Content-Encoding header found")
                handleEncodedResponse(response)
            }
        return response
    }

    fun postLargeResponseCompressed(): LargeResponse? {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipOutputStream ->
            val json = this.objectMapper.writeValueAsString(this.createLargeResponse());
            gzipOutputStream.write(json.toByteArray(), 0, json.toByteArray().size)
            gzipOutputStream.close()
        }
        val content = outputStream.toByteArray()
        logger.info("Sending post request with content length: ${content.size} bytes")
        val response = restClient.post().uri("http://localhost:8080/rest/demo/large-response")
            .header(HttpHeaders.CONTENT_ENCODING, "gzip")
            .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(content)
            .exchange { request, response ->
                handleEncodedResponse(response)
            }
        return response
    }

    private fun createLargeResponse(): LargeResponse {
        // Same implementation as in ResponseService
    }

    private fun handleEncodedResponse(response: RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse): LargeResponse? {
        var result: LargeResponse? = null
        val responseContent = response.body.readAllBytes()
        logger.info("Response content length: ${responseContent.size} bytes")
        if (response.headers.get(HttpHeaders.CONTENT_ENCODING)?.contains("gzip") == true) {
            result = this.objectMapper.readValue(GZIPInputStream(ByteArrayInputStream( responseContent)), LargeResponse::class.java)
        } else if (response.headers.get(HttpHeaders.CONTENT_ENCODING)?.contains("deflate") == true) {
            result = this.objectMapper.readValue(DeflaterInputStream(ByteArrayInputStream( responseContent)), LargeResponse::class.java)
        } else if (response.headers.get(HttpHeaders.CONTENT_ENCODING) == null || response.headers.get(HttpHeaders.CONTENT_ENCODING)
                ?.isEmpty() == true
        ) {
            result = this.objectMapper.readValue(responseContent, LargeResponse::class.java)
        } else {
            throw IllegalStateException("Unsupported content encoding: ${response.headers.get(HttpHeaders.CONTENT_ENCODING)}")
        }
        return result
    }
}
```
The `DemoRestClient` class first create a RestClient and an ObjectMapper to be used to call the Rest endpoints. 
The `handleEvent()` method is annotated with `@EventListener` to be called after the application has started. It 
calls the `getLargeResponse()` to send a plaintext request to the server, the `getLargeResponseCompressed()` to send a request for an compressed response
and `postLargeResponseCompressed()` to send a compressed body in the request and receive a compressed body in the response. 

The `getLargeResponse()` method sends a GET request with the `Accept-Encoding: identity` header to the Rest endpoint to recieve a plaintext response.
The `getLargeResponseCompressed()` method sends a GET request with the `Accept-Encoding: gzip, deflate` header to the Rest endpoint to receive a compressed response.
The `postLargeResponseCompressed()` method sends a POST request with the `Accept-Encoding: gzip, deflate` header, a `Content-Encoding: gzip` header, a `Content-Type: application/json` header and a compressed body expects a compressed response.
All three methods use the `handleEncodedResponse()` method to handle the response and return the `LargeResponse` object. 
The `handleEncodedResponse()` method checks the `Content-Encoding` header of the response and decompresses the response body accordingly.
The ObjectMapper is then used to deserialize the response body into a `LargeResponse` object and then returned.

### Conclusion Rest client
The rest client looks like more work, but it can be implemented in a generic way to handle the additional headers and the compression/decompression of the bodies. The developer 
would just provide the url the request content and the response type. The rest client can be really userfriendly and easy to use.

## Test results
- The size of the Json response is 5488892 bytes. 
- The size of the gRPC response is 3243294 bytes.
- The size of the compressed Json is 2747233 bytes. (The size is unusually large because of the random strings and numbers used in the response. A compression of 70% or more is normal for Json strings.)
- Json is a verbose format that is human readable but causes a large response size.
- gRPC is a binary format that is not human readable with a smaller response size and probably faster processing time.

The compressed Json response is smaller than both alternatives, but probably the processing time is slower.

## Conclusion
The right choice of the protocol depends on the use case. 
- If the amount of calls to the endpoint is low then an optimization is not needed and the human readable Json format will save time during development and maintainance.
- If the amount of calls is high and the request/response size is large then compressed Json is a good choice to reduce latency and traffic volume. It preserves the human readability of the Json format and has the lowest network load.
- If the amount of calls is high and the request/response size is large and the service is cpu constrained then gRPC is a good choice to reduce network load with low cpu load. It is not human readable but has a smaller response size and faster processing time. 