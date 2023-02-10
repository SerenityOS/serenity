serenity_component(
    ContentAccessServer
    RECOMMENDED
    TARGETS ContentAccessServer
)

compile_ipc(ContentAccessServer.ipc ContentAccessServerEndpoint.h)
compile_ipc(ContentAccessClient.ipc ContentAccessClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ContentAccessServerEndpoint.h
    ContentAccessClientEndpoint.h
)

serenity_bin(ContentAccessServer)
target_link_libraries(ContentAccessServer PRIVATE LibCore LibIPC LibGfx LibGUI LibMain LibFileSystemAccessClient LibProtocol)
add_dependencies(ContentAccessServer WindowServer RequestServer)
