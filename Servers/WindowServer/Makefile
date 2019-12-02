include ../../Makefile.common

OBJS = \
    WSEventLoop.o \
    WSWindow.o \
    WSWindowManager.o \
    WSScreen.o \
    WSMenuBar.o \
    WSMenu.o \
    WSMenuItem.o \
    WSClientConnection.o \
    WSWindowSwitcher.o \
    WSClipboard.o \
    WSCursor.o \
    WSWindowFrame.o \
    WSButton.o \
    WSCPUMonitor.o \
    WSCompositor.o \
    WSMenuManager.o \
    main.o

APP = WindowServer

DEFINES += -DUSERLAND

all: $(APP)

*.cpp: WindowServerEndpoint.h WindowClientEndpoint.h

WindowServerEndpoint.h: WindowServer.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

WindowClientEndpoint.h: WindowClient.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lc -ldraw -lcore -lthread -lpthread -laudio -lipc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d WindowServerEndpoint.h WindowClientEndpoint.h

