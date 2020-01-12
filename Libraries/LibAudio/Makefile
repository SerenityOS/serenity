OBJS = \
    AClientConnection.o \
    AWavLoader.o

LIBRARY = libaudio.a

AClientConnection.cpp: ../../Servers/AudioServer/AudioClientEndpoint.h

../../Servers/AudioServer/AudioClientEndpoint.h: ../../Servers/AudioServer/AudioClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibAudio/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibAudio/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
