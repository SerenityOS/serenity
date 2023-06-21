set(SOURCES
    Request.cpp
    RequestClient.cpp
    WebSocket.cpp
    WebSocketClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/RequestServer/RequestClientEndpoint.h
    ../../Services/RequestServer/RequestServerEndpoint.h
    ../../Services/WebSocket/WebSocketClientEndpoint.h
    ../../Services/WebSocket/WebSocketServerEndpoint.h
)

serenity_lib(LibProtocol protocol)
target_link_libraries(LibProtocol PRIVATE LibCore LibIPC)
