OBJS = \
    main.o \
    ClientConnection.o \
    Launcher.o

PROGRAM = LaunchServer

LIB_DEPS = Core IPC

EXTRA_CLEAN = LaunchServerEndpoint.h LaunchClientEndpoint.h

*.cpp: LaunchServerEndpoint.h LaunchClientEndpoint.h

LaunchServerEndpoint.h: LaunchServer.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

LaunchClientEndpoint.h: LaunchClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LaunchServer/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LaunchServer/

include ../../Makefile.common
