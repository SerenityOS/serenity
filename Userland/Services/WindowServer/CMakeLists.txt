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
    KeymapSwitcher.cpp
)

serenity_bin(WindowServer)
target_link_libraries(WindowServer LibCore LibGfx LibThreading LibIPC LibMain)
serenity_install_headers(Services/WindowServer)
