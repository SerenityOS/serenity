serenity_component(
    LaunchServer
    REQUIRED
    TARGETS LaunchServer
)

compile_ipc(LaunchServer.ipc LaunchServerEndpoint.h)
compile_ipc(LaunchClient.ipc LaunchClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    Launcher.cpp
    main.cpp
)

set(GENERATED_SOURCES
    LaunchClientEndpoint.h
    LaunchServerEndpoint.h
)

serenity_bin(LaunchServer)
target_link_libraries(LaunchServer PRIVATE LibCore LibIPC LibDesktop LibFileSystem LibMain LibURL)
