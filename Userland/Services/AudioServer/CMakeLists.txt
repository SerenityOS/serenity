compile_ipc(AudioServer.ipc AudioServerEndpoint.h)
compile_ipc(AudioClient.ipc AudioClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    Mixer.cpp
    main.cpp
    AudioServerEndpoint.h
    AudioClientEndpoint.h
)

serenity_bin(AudioServer)
target_link_libraries(AudioServer LibCore LibThread LibIPC)
