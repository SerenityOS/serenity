OBJS = \
    Download.o \
    Client.o

LIBRARY = libprotocol.a


Download.cpp: ../../Servers/ProtocolServer/ProtocolClientEndpoint.h ../../Servers/ProtocolServer/ProtocolServerEndpoint.h

../../Servers/ProtocolServer/ProtocolClientEndpoint.h: ../../Servers/ProtocolServer/ProtocolClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

../../Servers/ProtocolServer/ProtocolServerEndpoint.h: ../../Servers/ProtocolServer/ProtocolServer.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@


include ../../Makefile.common
