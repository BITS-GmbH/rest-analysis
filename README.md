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

## 