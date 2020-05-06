compile_ipc(LaunchServer.ipc LaunchServerEndpoint.h)
compile_ipc(LaunchClient.ipc LaunchClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    Launcher.cpp
    main.cpp
    LaunchClientEndpoint.h
    LaunchServerEndpoint.h
)

serenity_bin(LaunchServer)
target_link_libraries(LaunchServer LibCore LibIPC)
