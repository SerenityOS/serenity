compile_ipc(ImageDecoderServer.ipc ImageDecoderServerEndpoint.h)
compile_ipc(ImageDecoderClient.ipc ImageDecoderClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    ImageDecoderServerEndpoint.h
    ImageDecoderClientEndpoint.h
)

serenity_bin(ImageDecoder)
target_link_libraries(ImageDecoder LibGfx LibIPC)
