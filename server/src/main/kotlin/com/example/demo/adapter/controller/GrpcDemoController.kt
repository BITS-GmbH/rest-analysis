/**
 *    Copyright 2025 Bits GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
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