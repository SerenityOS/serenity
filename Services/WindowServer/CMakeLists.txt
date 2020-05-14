compile_ipc(WindowServer.ipc WindowServerEndpoint.h)
compile_ipc(WindowClient.ipc WindowClientEndpoint.h)

set(SOURCES
    AppletManager.cpp
    Button.cpp
    ClientConnection.cpp
    Compositor.cpp
    Cursor.cpp
    EventLoop.cpp
    main.cpp
    MenuBar.cpp
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
)

serenity_bin(WindowServer)
target_link_libraries(WindowServer LibCore LibGfx LibThread LibPthread LibIPC)
