set(SOURCES
    SampleFormats.cpp
    Loader.cpp
    WavLoader.cpp
    FlacLoader.cpp
    WavWriter.cpp
    MP3Loader.cpp
    UserSampleQueue.cpp
)

if (SERENITYOS)
    list(APPEND SOURCES ConnectionToServer.cpp)
    set(GENERATED_SOURCES
        ../../Services/AudioServer/AudioClientEndpoint.h
        ../../Services/AudioServer/AudioServerEndpoint.h
    )
endif()

serenity_lib(LibAudio audio)
target_link_libraries(LibAudio LibCore LibIPC LibThreading)
