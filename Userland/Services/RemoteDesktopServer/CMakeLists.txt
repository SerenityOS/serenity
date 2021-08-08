serenity_component(
    RemoteDesktopServer
    TARGETS RemoteDesktopServer
)

set(SOURCES
    Client.cpp
    Configuration.cpp
    GfxClient.cpp
    main.cpp
    Server.cpp
)

set(GENERATED_SOURCES
    ../../Libraries/LibRemoteDesktop/RemoteCompositorClientEndpoint.h
    ../../Libraries/LibRemoteDesktop/RemoteCompositorServerEndpoint.h
    ../../Libraries/LibRemoteDesktop/RemoteDesktopClientEndpoint.h
    ../../Libraries/LibRemoteDesktop/RemoteDesktopServerEndpoint.h
    ../../Libraries/LibGfx/Remote/RemoteGfxClientEndpoint.h
    ../../Libraries/LibGfx/Remote/RemoteGfxServerEndpoint.h
)

serenity_bin(RemoteDesktopServer)
target_link_libraries(RemoteDesktopServer LibCore LibRemoteDesktop)
