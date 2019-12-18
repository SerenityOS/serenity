OBJS = \
    PSClientConnection.o \
    Protocol.o \
    Download.o \
    HttpProtocol.o \
    HttpDownload.o \
    main.o

PROGRAM = ProtocolServer

LDFLAGS = -lc -lcore -lipc

*.cpp: ProtocolServerEndpoint.h ProtocolClientEndpoint.h

ProtocolServerEndpoint.h: ProtocolServer.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

ProtocolClientEndpoint.h: ProtocolClient.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

include ../../Makefile.common
