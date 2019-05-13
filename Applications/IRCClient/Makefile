include ../../Makefile.common

OBJS = \
    IRCClient.o \
    IRCChannel.o \
    IRCQuery.o \
    IRCLogBuffer.o \
    IRCLogBufferModel.o \
    IRCAppWindow.o \
    IRCWindow.o \
    IRCWindowListModel.o \
    IRCChannelMemberListModel.o \
    main.o

APP = IRCClient

DEFINES += -DUSERLAND

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -lcore -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APP) $(OBJS) *.d

