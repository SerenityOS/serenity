compile_ipc(ServiceManagementServer.ipc ServiceManagementServerEndpoint.h)
compile_ipc(ServiceManagementClient.ipc ServiceManagementClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    Service.cpp
    ServiceManagement.cpp
    ServiceManagementServerEndpoint.h
    ServiceManagementClientEndpoint.h
)

serenity_bin(SystemServer)
target_link_libraries(SystemServer LibCore LibIPC)
