serenity_component(
    RemoteDesktopClient
    REQUIRED
    TARGETS RemoteDesktopClient
)

set(SOURCES
    main.cpp
    RemoteDesktopWidget.cpp
    RemoteDesktopClientGML.h
)

compile_gml(RemoteDesktopClient.gml RemoteDesktopClientGML.h remote_desktop_client_gml)

serenity_bin(RemoteDesktopClient)
target_link_libraries(RemoteDesktopClient LibGUI LibRemoteDesktop)
