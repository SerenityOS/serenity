compile_ipc(DownloadServer.ipc DownloadServerEndpoint.h)
compile_ipc(DownloadClient.ipc DownloadClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    Download.cpp
    DownloadClientEndpoint.h
    DownloadServerEndpoint.h
    GeminiDownload.cpp
    GeminiProtocol.cpp
    HttpDownload.cpp
    HttpProtocol.cpp
    HttpsDownload.cpp
    HttpsProtocol.cpp
    main.cpp
    Protocol.cpp
)

serenity_bin(DownloadServer)
target_link_libraries(DownloadServer LibCore LibIPC LibGemini LibHTTP)
