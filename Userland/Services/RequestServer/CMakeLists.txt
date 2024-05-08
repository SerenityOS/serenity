serenity_component(
    RequestServer
    TARGETS RequestServer
)

compile_ipc(RequestServer.ipc RequestServerEndpoint.h)
compile_ipc(RequestClient.ipc RequestClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    ConnectionCache.cpp
    Request.cpp
    GeminiRequest.cpp
    GeminiProtocol.cpp
    HttpRequest.cpp
    HttpProtocol.cpp
    HttpsRequest.cpp
    HttpsProtocol.cpp
    main.cpp
    Protocol.cpp
)

set(GENERATED_SOURCES
    RequestClientEndpoint.h
    RequestServerEndpoint.h
)

serenity_bin(RequestServer)
target_link_libraries(RequestServer PRIVATE LibCore LibCrypto LibIPC LibGemini LibHTTP LibMain LibTLS LibWebSocket LibURL LibThreading)
