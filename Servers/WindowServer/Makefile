OBJS = \
    WSEventLoop.o \
    WSWindow.o \
    WSWindowManager.o \
    WSScreen.o \
    WSMenuBar.o \
    WSMenu.o \
    WSMenuItem.o \
    WSClientConnection.o \
    WSWindowSwitcher.o \
    WSClipboard.o \
    WSCursor.o \
    WSWindowFrame.o \
    WSButton.o \
    WSCompositor.o \
    WSMenuManager.o \
    main.o

PROGRAM = WindowServer

LDFLAGS = -lc -ldraw -lcore -lthread -lpthread -lipc

*.cpp: WindowServerEndpoint.h WindowClientEndpoint.h

WindowServerEndpoint.h: WindowServer.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

WindowClientEndpoint.h: WindowClient.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

EXTRA_CLEAN = WindowServerEndpoint.h WindowClientEndpoint.h

install:
	mkdir -p ../../Root/usr/include/WindowServer/
	cp *.h ../../Root/usr/include/WindowServer/

include ../../Makefile.common
