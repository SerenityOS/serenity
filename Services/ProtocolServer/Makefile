OBJS = \
    PSClientConnection.o \
    Protocol.o \
    Download.o \
    HttpProtocol.o \
    HttpDownload.o \
    HttpsProtocol.o \
    HttpsDownload.o \
    main.o

PROGRAM = ProtocolServer

LIB_DEPS = HTTP TLS Crypto Core IPC

EXTRA_CLEAN = ProtocolServerEndpoint.h ProtocolClientEndpoint.h

*.cpp: ProtocolServerEndpoint.h ProtocolClientEndpoint.h

ProtocolServerEndpoint.h: ProtocolServer.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

ProtocolClientEndpoint.h: ProtocolClient.ipc | IPCCOMPILER
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

include ../../Makefile.common
