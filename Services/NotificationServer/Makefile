OBJS = \
    main.o \
    ClientConnection.o \
    NotificationWindow.o

PROGRAM = NotificationServer

LIB_DEPS = GUI Gfx Core IPC

EXTRA_CLEAN = NotificationServerEndpoint.h NotificationClientEndpoint.h

*.cpp: NotificationServerEndpoint.h NotificationClientEndpoint.h

NotificationServerEndpoint.h: NotificationServer.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

NotificationClientEndpoint.h: NotificationClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/NotificationServer/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/NotificationServer/

include ../../Makefile.common
