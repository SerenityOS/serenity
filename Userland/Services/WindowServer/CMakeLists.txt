compile_ipc(WindowServer.ipc WindowServerEndpoint.h)
compile_ipc(WindowClient.ipc WindowClientEndpoint.h)
compile_ipc(WindowManagerServer.ipc WindowManagerServerEndpoint.h)
compile_ipc(WindowManagerClient.ipc WindowManagerClientEndpoint.h)

set(SOURCES
    AppletManager.cpp
    Button.cpp
    ClientConnection.cpp
    Compositor.cpp
    Cursor.cpp
    EventLoop.cpp
    main.cpp
    Menubar.cpp
    Menu.cpp
    MenuItem.cpp
    MenuManager.cpp
    Screen.cpp
    Window.cpp
    WindowFrame.cpp
    WindowManager.cpp
    WindowSwitcher.cpp
    WindowServerEndpoint.h
    WindowClientEndpoint.h
    WindowManagerServerEndpoint.h
    WindowManagerClientEndpoint.h
    WMClientConnection.cpp
)

serenity_bin(WindowServer)
target_link_libraries(WindowServer LibCore LibGfx LibThreading LibIPC)
serenity_install_headers(Services/WindowServer)
