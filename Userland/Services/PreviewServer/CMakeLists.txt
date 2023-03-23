serenity_component(
    PreviewServer
    REQUIRED
    TARGETS PreviewServer
)

compile_ipc(PreviewServer.ipc PreviewServerEndpoint.h)
compile_ipc(PreviewClient.ipc PreviewClientEndpoint.h)

set(SOURCES
    Cache.cpp
    ConnectionFromClient.cpp
    Error.cpp
    PreviewProvider.cpp
    PreviewProviders/Image.cpp
    main.cpp
)

set(GENERATED_SOURCES
    PreviewServerEndpoint.h
    PreviewClientEndpoint.h
)

serenity_bin(PreviewServer)
target_link_libraries(PreviewServer PRIVATE LibCore LibFileSystem LibThreading LibIPC LibGfx LibImageDecoderClient LibMain LibCrypto)
