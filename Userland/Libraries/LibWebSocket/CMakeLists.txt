set(SOURCES
    ConnectionInfo.cpp
    Impl/AbstractWebSocketImpl.cpp
    Impl/TCPWebSocketConnectionImpl.cpp
    Impl/TLSv12WebSocketConnectionImpl.cpp
    WebSocket.cpp
)

serenity_lib(LibWebSocket websocket)
target_link_libraries(LibWebSocket LibCore LibCrypto LibTLS)
