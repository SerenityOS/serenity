set(SOURCES
    Client.cpp
)

set(GENERATED_SOURCES
    ../../Services/ImageDecoder/ImageDecoderClientEndpoint.h
    ../../Services/ImageDecoder/ImageDecoderServerEndpoint.h
)

serenity_lib(LibImageDecoderClient imagedecoderclient)
target_link_libraries(LibImageDecoderClient LibIPC LibGfx)
