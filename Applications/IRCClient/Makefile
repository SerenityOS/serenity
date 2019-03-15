OBJS = \
    IRCClient.o \
    IRCChannel.o \
    IRCQuery.o \
    IRCLogBuffer.o \
    IRCLogBufferModel.o \
    IRCAppWindow.o \
    IRCClientWindow.o \
    IRCClientWindowListModel.o \
    main.o

APP = IRCClient

STANDARD_FLAGS = -std=c++17
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I../.. -I. -I../../LibC

DEFINES = -DSERENITY -DSANITIZE_PTRS -DUSERLAND

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(FLAVOR_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-ld
AR = i686-pc-serenity-ar
LDFLAGS = -L../../LibC -L../../LibGUI

all: $(APP)

$(APP): $(OBJS)
	$(LD) -o $(APP) $(LDFLAGS) $(OBJS) -lgui -lc

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

