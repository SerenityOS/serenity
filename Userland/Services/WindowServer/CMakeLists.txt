serenity_component(
    WindowServer
    REQUIRED
    TARGETS WindowServer
)

compile_ipc(WindowServer.ipc WindowServerEndpoint.h)
compile_ipc(WindowClient.ipc WindowClientEndpoint.h)
compile_ipc(WindowManagerServer.ipc WindowManagerServerEndpoint.h)
compile_ipc(WindowManagerClient.ipc WindowManagerClientEndpoint.h)

set(SOURCES
    Animation.cpp
    AppletManager.cpp
    Button.cpp
    ClientConnection.cpp
    Compositor.cpp
    Cursor.cpp
    EventLoop.cpp
    main.cpp
    Menu.cpp
    Menubar.cpp
    MenuItem.cpp
    MenuManager.cpp
    MultiScaleBitmaps.cpp
    Overlays.cpp
    RemoteCompositorClientConnection.cpp
    Screen.cpp
    ScreenLayout.cpp
    Window.cpp
    WindowFrame.cpp
    WindowManager.cpp
    WindowStack.cpp
    WindowSwitcher.cpp
    WindowServerEndpoint.h
    WindowClientEndpoint.h
    WindowManagerServerEndpoint.h
    WindowManagerClientEndpoint.h
    WMClientConnection.cpp
)

set(GENERATED_SOURCES
    ../../Libraries/LibRemoteDesktop/RemoteCompositorClientEndpoint.h
    ../../Libraries/LibRemoteDesktop/RemoteCompositorServerEndpoint.h
)

serenity_bin(WindowServer)
target_link_libraries(WindowServer LibCore LibGfx LibThreading LibRemoteDesktop LibIPC LibMain)
serenity_install_headers(Services/WindowServer)
