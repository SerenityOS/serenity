serenity_component(
    WebSocket
    TARGETS WebSocket
)

compile_ipc(WebSocketServer.ipc WebSocketServerEndpoint.h)
compile_ipc(WebSocketClient.ipc WebSocketClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    WebSocketClientEndpoint.h
    WebSocketServerEndpoint.h
)

serenity_bin(WebSocket)
target_link_libraries(WebSocket LibCore LibIPC LibWebSocket LibMain)
