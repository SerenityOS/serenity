OBJS = \
    AppletManager.o \
    Button.o \
    ClientConnection.o \
    Clipboard.o \
    Compositor.o \
    Cursor.o \
    EventLoop.o \
    Menu.o \
    MenuBar.o \
    MenuItem.o \
    MenuManager.o \
    Screen.o \
    Window.o \
    WindowFrame.o \
    WindowManager.o \
    WindowSwitcher.o \
    main.o

PROGRAM = WindowServer

LIB_DEPS = Gfx Core Thread Pthread IPC

*.cpp: WindowServerEndpoint.h WindowClientEndpoint.h

WindowServerEndpoint.h: WindowServer.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

WindowClientEndpoint.h: WindowClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

EXTRA_CLEAN = WindowServerEndpoint.h WindowClientEndpoint.h

install:
	mkdir -p ../../Root/usr/include/WindowServer/
	cp *.h ../../Root/usr/include/WindowServer/

include ../../Makefile.common
