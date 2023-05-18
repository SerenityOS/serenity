set(SOURCES
    GenericTypes.cpp
    SampleFormats.cpp
    Loader.cpp
    WavLoader.cpp
    FlacLoader.cpp
    WavWriter.cpp
    Metadata.cpp
    MP3Loader.cpp
    QOALoader.cpp
    QOATypes.cpp
    UserSampleQueue.cpp
    VorbisComment.cpp
)

if (SERENITYOS)
    list(APPEND SOURCES ConnectionToServer.cpp)
    set(GENERATED_SOURCES
        ../../Services/AudioServer/AudioClientEndpoint.h
        ../../Services/AudioServer/AudioServerEndpoint.h
    )
endif()

serenity_lib(LibAudio audio)
target_link_libraries(LibAudio PRIVATE LibCore LibIPC LibThreading LibUnicode LibCrypto)
