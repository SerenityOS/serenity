serenity_component(
    AudioServer
    RECOMMENDED
    TARGETS AudioServer
)

compile_ipc(AudioServer.ipc AudioServerEndpoint.h)
compile_ipc(AudioClient.ipc AudioClientEndpoint.h)
compile_ipc(AudioManagerClient.ipc AudioManagerClientEndpoint.h)
compile_ipc(AudioManagerServer.ipc AudioManagerServerEndpoint.h)

set(SOURCES
    ClientAudioStream.cpp
    ConnectionFromClient.cpp
    ConnectionFromManagerClient.cpp
    Mixer.cpp
    main.cpp
)

set(GENERATED_SOURCES
    AudioServerEndpoint.h
    AudioClientEndpoint.h
    AudioManagerClientEndpoint.h
    AudioManagerServerEndpoint.h
)

serenity_bin(AudioServer)
target_link_libraries(AudioServer PRIVATE LibCore LibThreading LibIPC LibMain)
