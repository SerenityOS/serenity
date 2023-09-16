set(SOURCES
    "${SERENITY_SOURCE_DIR}/Userland/Services/WebSocket/ConnectionFromClient.cpp"
)

if (ANDROID)
   add_library(websocket SHARED
       ${SOURCES}
       ../Android/src/main/cpp/WebSocketService.cpp
       ../Android/src/main/cpp/LadybirdServiceBaseJNI.cpp
       ../Utilities.cpp
   )
else()
    add_library(websocket STATIC ${SOURCES})
endif()
add_executable(WebSocketServer main.cpp)
target_link_libraries(WebSocketServer PRIVATE websocket)
set_target_properties(WebSocketServer PROPERTIES OUTPUT_NAME WebSocket)
target_link_libraries(websocket PUBLIC LibCore LibFileSystem LibIPC LibMain LibTLS LibWebSocket LibWebView)
