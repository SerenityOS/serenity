serenity_component(
    SessionServer
    REQUIRED
    TARGETS SessionServer
)

compile_ipc(SessionExitInhibitionServer.ipc SessionExitInhibitionServerEndpoint.h)
compile_ipc(SessionExitInhibitionClient.ipc SessionExitInhibitionClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    SessionExitInhibitionClientEndpoint.h
    SessionExitInhibitionServerEndpoint.h
    main.cpp
    SessionExitInhibitor.cpp
)

serenity_bin(SessionServer)
target_link_libraries(SessionServer LibIPC LibMain)
