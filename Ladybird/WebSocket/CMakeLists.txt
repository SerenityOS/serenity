set(SOURCES
    "${SERENITY_SOURCE_DIR}/Userland/Services/WebSocket/ConnectionFromClient.cpp"
)

if (ANDROID)
   add_library(websocket_svc SHARED
       ${SOURCES}
       ../Android/src/main/cpp/WebSocketService.cpp
       ../Android/src/main/cpp/LadybirdServiceBaseJNI.cpp
       ../Utilities.cpp
   )
   set_target_properties(websocket_svc PROPERTIES OUTPUT_NAME "websocket_svc")
else()
    add_library(websocket_svc STATIC ${SOURCES})
endif()
add_executable(WebSocketServer main.cpp)
target_link_libraries(WebSocketServer PRIVATE websocket_svc)
set_target_properties(WebSocketServer PROPERTIES OUTPUT_NAME WebSocket)
target_link_libraries(websocket_svc PUBLIC LibCore LibFileSystem LibIPC LibMain LibTLS LibWebSocket LibWebView)
set_helper_process_properties(WebSocketServer)
