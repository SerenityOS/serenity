set(SOURCES
    Client.cpp
    Download.cpp
    WebSocket.cpp
    WebSocketClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/ProtocolServer/ProtocolClientEndpoint.h
    ../../Services/ProtocolServer/ProtocolServerEndpoint.h
    ../../Services/WebSocket/WebSocketClientEndpoint.h
    ../../Services/WebSocket/WebSocketServerEndpoint.h
)

serenity_lib(LibProtocol protocol)
target_link_libraries(LibProtocol LibIPC)
