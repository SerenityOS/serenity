serenity_component(
    NotificationServer
    REQUIRED
    TARGETS NotificationServer
)

compile_gml(NotificationWidget.gml NotificationWidgetGML.cpp)
compile_ipc(NotificationServer.ipc NotificationServerEndpoint.h)
compile_ipc(NotificationClient.ipc NotificationClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    main.cpp
    NotificationWidgetGML.cpp
    NotificationWindow.cpp
)

set(GENERATED_SOURCES
    NotificationServerEndpoint.h
    NotificationClientEndpoint.h
)

serenity_bin(NotificationServer)
target_link_libraries(NotificationServer PRIVATE LibCore LibDesktop LibGfx LibGUI LibIPC LibMain LibURL)
