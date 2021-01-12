compile_ipc(NotificationServer.ipc NotificationServerEndpoint.h)
compile_ipc(NotificationClient.ipc NotificationClientEndpoint.h)

set(SOURCES
    ClientConnection.cpp
    main.cpp
    NotificationWindow.cpp
    NotificationServerEndpoint.h
    NotificationClientEndpoint.h
)

serenity_bin(NotificationServer)
target_link_libraries(NotificationServer LibGUI LibIPC)
