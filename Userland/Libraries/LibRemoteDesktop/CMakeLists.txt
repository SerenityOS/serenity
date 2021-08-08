set(SOURCES
    RemoteCompositor.cpp
    RemoteCompositorServerConnection.cpp
    RemoteDesktopRenderer.cpp
)

compile_ipc(RemoteCompositorClient.ipc RemoteCompositorClientEndpoint.h)
compile_ipc(RemoteCompositorServer.ipc RemoteCompositorServerEndpoint.h)
compile_ipc(RemoteDesktopClient.ipc RemoteDesktopClientEndpoint.h)
compile_ipc(RemoteDesktopServer.ipc RemoteDesktopServerEndpoint.h)

set(GENERATED_SOURCES
    RemoteCompositorClientEndpoint.h
    RemoteCompositorServerEndpoint.h
    RemoteDesktopClientEndpoint.h
    RemoteDesktopServerEndpoint.h
)

serenity_lib(LibRemoteDesktop remotedesktop)
target_link_libraries(LibRemoteDesktop LibGfx LibIPC)
