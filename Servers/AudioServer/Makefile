OBJS = \
    main.o \
    ASMixer.o \
    ASClientConnection.o \
    ASEventLoop.o

PROGRAM = AudioServer

LDFLAGS = -lc -lcore -lipc -lthread -lpthread

EXTRA_CLEAN = AudioServerEndpoint.h AudioClientEndpoint.h

*.cpp: AudioServerEndpoint.h AudioClientEndpoint.h

AudioServerEndpoint.h: AudioServer.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

AudioClientEndpoint.h: AudioClient.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/AudioServer/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/AudioServer/

include ../../Makefile.common
