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
)

set(GENERATED_SOURCES
    AudioServerEndpoint.h
    AudioClientEndpoint.h
)

serenity_bin(AudioServer)
target_link_libraries(AudioServer PRIVATE LibCore LibThreading LibIPC LibMain)
