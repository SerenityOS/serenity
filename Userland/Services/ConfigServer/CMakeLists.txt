serenity_component(
    ConfigServer
    REQUIRED
    TARGETS ConfigServer
)

compile_ipc(ConfigServer.ipc ConfigServerEndpoint.h)
compile_ipc(ConfigClient.ipc ConfigClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    ConfigServerEndpoint.h
    ConfigClientEndpoint.h
)

serenity_bin(ConfigServer)
target_link_libraries(ConfigServer LibIPC LibMain)
