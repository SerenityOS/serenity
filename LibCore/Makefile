OBJS = \
    CIODevice.o \
    CFile.o \
    CSocket.o \
    CTCPSocket.o \
    CElapsedTimer.o \
    CNotifier.o \
    CHttpRequest.o \
    CHttpResponse.o \
    CHttpJob.o \
    CNetworkJob.o \
    CNetworkResponse.o \
    CObject.o \
    CTimer.o \
    CEventLoop.o \
    CEvent.o

LIBRARY = libcore.a
STANDARD_FLAGS = -std=c++17 -Wno-sized-deallocation
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -fno-exceptions -fno-rtti
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I../LibC -I../Servers -I.. -I.

DEFINES = -DSERENITY -DUSERLAND -DSANITIZE_PTRS -DLIBGUI

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(FLAVOR_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-ld
AR = i686-pc-serenity-ar

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS) $(LIBS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d

