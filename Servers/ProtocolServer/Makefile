OBJS = \
    PSClientConnection.o \
    Protocol.o \
    Download.o \
    HttpProtocol.o \
    HttpDownload.o \
    main.o

PROGRAM = ProtocolServer

LIB_DEPS = Core IPC

*.cpp: ProtocolServerEndpoint.h ProtocolClientEndpoint.h

ProtocolServerEndpoint.h: ProtocolServer.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

ProtocolClientEndpoint.h: ProtocolClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

include ../../Makefile.common
