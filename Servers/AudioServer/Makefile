include ../../Makefile.common

AUDIOSERVER_OBJS = \
    main.o \
    ASMixer.o \
    ASClientConnection.o \
    ASEventLoop.o

APP = AudioServer
OBJS = $(AUDIOSERVER_OBJS)

DEFINES += -DUSERLAND

all: $(APP)

*.cpp: AudioServerEndpoint.h

AudioServerEndpoint.h: AudioServer.ipc
	@echo "IPC $<"; $(IPCCOMPILER) $< > $@

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lc -lcore -lipc -lthread -lpthread

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d AudioServerEndpoint.h

