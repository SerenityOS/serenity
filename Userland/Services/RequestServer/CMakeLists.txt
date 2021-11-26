serenity_component(
    RequestServer
    TARGETS RequestServer
)

compile_ipc(RequestServer.ipc RequestServerEndpoint.h)
compile_ipc(RequestClient.ipc RequestClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    ConnectionCache.cpp
    Request.cpp
    RequestClientEndpoint.h
    RequestServerEndpoint.h
    GeminiRequest.cpp
    GeminiProtocol.cpp
    HttpRequest.cpp
    HttpProtocol.cpp
    HttpsRequest.cpp
    HttpsProtocol.cpp
    main.cpp
    Protocol.cpp
)

serenity_bin(RequestServer)
target_link_libraries(RequestServer LibCore LibIPC LibGemini LibHTTP LibMain)
