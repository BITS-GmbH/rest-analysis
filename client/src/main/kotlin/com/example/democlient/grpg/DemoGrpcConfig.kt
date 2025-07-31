package com.example.democlient.grpg

import com.example.democlient.rpc.LargeResponseProviderGrpc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.grpc.client.GrpcChannelFactory


@Configuration
class DemoGrpcConfig {
    @Bean
    fun stub(channels: GrpcChannelFactory): LargeResponseProviderGrpc.LargeResponseProviderBlockingStub? {
        return LargeResponseProviderGrpc.newBlockingStub(channels.createChannel("large-response"))
    }
}