set(SOURCES
    Buffer.cpp
    ClientConnection.cpp
    Loader.cpp
    WavLoader.cpp
    WavWriter.cpp
    Mp3Id3.cpp
    Mp3Frame.cpp
    Mp3Loader.cpp
)

set(GENERATED_SOURCES
    ../../Services/AudioServer/AudioClientEndpoint.h
    ../../Services/AudioServer/AudioServerEndpoint.h
)

serenity_lib(LibAudio audio)
target_link_libraries(LibAudio LibCore LibIPC)
