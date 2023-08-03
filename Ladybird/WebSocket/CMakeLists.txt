set(SOURCES
    "${SERENITY_SOURCE_DIR}/Userland/Services/WebSocket/ConnectionFromClient.cpp"
    main.cpp
)

add_executable(WebSocketServer ${SOURCES})
set_target_properties(WebSocketServer PROPERTIES OUTPUT_NAME WebSocket)
target_link_libraries(WebSocketServer PRIVATE LibCore LibFileSystem LibIPC LibMain LibTLS LibWebSocket LibWebView)
