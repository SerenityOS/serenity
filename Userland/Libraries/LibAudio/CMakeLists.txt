set(SOURCES
    SampleFormats.cpp
    ConnectionToServer.cpp
    Loader.cpp
    WavLoader.cpp
    FlacLoader.cpp
    WavWriter.cpp
    MP3Loader.cpp
    UserSampleQueue.cpp
)

set(GENERATED_SOURCES
    ../../Services/AudioServer/AudioClientEndpoint.h
    ../../Services/AudioServer/AudioServerEndpoint.h
)

serenity_lib(LibAudio audio)
target_link_libraries(LibAudio LibCore LibIPC LibThreading)
