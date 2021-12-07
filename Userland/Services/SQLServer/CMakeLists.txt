serenity_component(
    SQLServer
    REQUIRED
    TARGETS SQLServer
)

compile_ipc(SQLServer.ipc SQLServerEndpoint.h)
compile_ipc(SQLClient.ipc SQLClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    DatabaseConnection.cpp
    main.cpp
    SQLClientEndpoint.h
    SQLServerEndpoint.h
    SQLStatement.cpp
    )

serenity_bin(SQLServer)
target_link_libraries(SQLServer LibCore LibIPC LibSQL LibMain)
