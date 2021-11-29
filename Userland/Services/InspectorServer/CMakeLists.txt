serenity_component(
    InspectorServer
    REQUIRED
    TARGETS InspectorServer
)

compile_ipc(InspectorServer.ipc InspectorServerEndpoint.h)
compile_ipc(InspectorClient.ipc InspectorClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    InspectableProcess.cpp
    InspectorServerEndpoint.h
    InspectorClientEndpoint.h
)

serenity_bin(InspectorServer)
target_link_libraries(InspectorServer LibIPC LibMain)
