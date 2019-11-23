include ../../Makefile.common

OBJS = \
    PSClientConnection.o \
    Protocol.o \
    Download.o \
    HttpProtocol.o \
    HttpDownload.o \
    main.o

APP = ProtocolServer

DEFINES += -DUSERLAND

all: $(APP)

*.cpp: ProtocolServerEndpoint.h ProtocolClientEndpoint.h

ProtocolServerEndpoint.h: ProtocolServer.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

ProtocolClientEndpoint.h: ProtocolClient.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lc -lcore -lipc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d ProtocolClientEndpoint.h ProtocolServerEndpoint.h

