serenity_component(
    WebSocket
    TARGETS WebSocketServer
)

compile_ipc(WebSocketServer.ipc WebSocketServerEndpoint.h)
compile_ipc(WebSocketClient.ipc WebSocketClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    main.cpp
)

set(GENERATED_SOURCES
    WebSocketClientEndpoint.h
    WebSocketServerEndpoint.h
)

# Note: We use a target name of WebSocketServer here to deconflict with the
#    Lagom namespaced target name for LibWebSocket, Lagom::WebSocket.
#    The server binary name is still WebSocket.
serenity_bin(WebSocketServer)
set_target_properties(WebSocketServer PROPERTIES OUTPUT_NAME WebSocket)
target_link_libraries(WebSocketServer LibCore LibIPC LibWebSocket LibMain)
