compile_ipc(ProtocolServer.ipc ProtocolServerEndpoint.h)
compile_ipc(ProtocolClient.ipc ProtocolClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    Download.cpp
    GeminiDownload.cpp
    GeminiProtocol.cpp
    HttpDownload.cpp
    HttpProtocol.cpp
    HttpsDownload.cpp
    HttpsProtocol.cpp
    main.cpp
    Protocol.cpp
    ProtocolServerEndpoint.h
    ProtocolClientEndpoint.h
)

serenity_bin(ProtocolServer)
target_link_libraries(ProtocolServer LibCore LibIPC LibGemini LibHTTP)
