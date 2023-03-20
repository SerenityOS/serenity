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
    ConnectionFromClient.cpp
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
    HardwareScreenBackend.cpp
    VirtualScreenBackend.cpp
    ScreenLayout.cpp
    Window.cpp
    WindowFrame.cpp
    WindowManager.cpp
    WindowStack.cpp
    WindowSwitcher.cpp
    WMConnectionFromClient.cpp
    KeymapSwitcher.cpp
)

set(GENERATED_SOURCES
    WindowServerEndpoint.h
    WindowClientEndpoint.h
    WindowManagerServerEndpoint.h
    WindowManagerClientEndpoint.h
)

serenity_bin(WindowServer)
target_link_libraries(WindowServer PRIVATE LibCore LibFileSystem LibKeyboard LibGfx LibThreading LibIPC LibMain)
serenity_install_headers(Services/WindowServer)
