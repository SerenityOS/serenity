set(SOURCES
    Download.cpp
    DownloadClient.cpp
    WebSocket.cpp
    WebSocketClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/DownloadServer/DownloadClientEndpoint.h
    ../../Services/DownloadServer/DownloadServerEndpoint.h
    ../../Services/WebSocket/WebSocketClientEndpoint.h
    ../../Services/WebSocket/WebSocketServerEndpoint.h
)

serenity_lib(LibProtocol protocol)
target_link_libraries(LibProtocol LibIPC)
