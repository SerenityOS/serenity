set(SOURCES
    ConnectionInfo.cpp
    Impl/WebSocketImpl.cpp
    WebSocket.cpp
)

serenity_lib(LibWebSocket websocket)
target_link_libraries(LibWebSocket LibCore LibCrypto LibTLS)
