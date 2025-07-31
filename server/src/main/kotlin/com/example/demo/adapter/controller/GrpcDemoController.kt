package com.example.demo.adapter.controller

import com.example.demo.usecase.service.ResponseService
import com.example.democlient.rpc.Empty
import com.example.democlient.rpc.KeyValuePair
import com.example.democlient.rpc.LargeResponse
import com.example.democlient.rpc.LargeResponseProviderGrpc
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

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