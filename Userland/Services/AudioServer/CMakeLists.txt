serenity_component(
    AudioServer
    RECOMMENDED
    TARGETS AudioServer
)

compile_ipc(AudioServer.ipc AudioServerEndpoint.h)
compile_ipc(AudioClient.ipc AudioClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    Mixer.cpp
    main.cpp
    AudioServerEndpoint.h
    AudioClientEndpoint.h
)

serenity_bin(AudioServer)
target_link_libraries(AudioServer LibCore LibThreading LibIPC LibMain)
