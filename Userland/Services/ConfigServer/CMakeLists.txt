serenity_component(
    ConfigServer
    REQUIRED
    TARGETS ConfigServer
)

compile_ipc(ConfigServer.ipc ConfigServerEndpoint.h)
compile_ipc(ConfigClient.ipc ConfigClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ConfigServerEndpoint.h
    ConfigClientEndpoint.h
)

serenity_bin(ConfigServer)
target_link_libraries(ConfigServer PRIVATE LibCore LibIPC LibMain)
