OBJS = \
    main.o \
    ASMixer.o \
    ASClientConnection.o

PROGRAM = AudioServer

LIB_DEPS = Core IPC Thread Pthread

EXTRA_CLEAN = AudioServerEndpoint.h AudioClientEndpoint.h

*.cpp: AudioServerEndpoint.h AudioClientEndpoint.h

AudioServerEndpoint.h: AudioServer.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

AudioClientEndpoint.h: AudioClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/AudioServer/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/AudioServer/

include ../../Makefile.common
