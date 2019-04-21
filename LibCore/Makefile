include ../Makefile.common

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
    CConfigFile.o \
    CEvent.o

LIBRARY = libcore.a
DEFINES += -DUSERLAND

all: $(LIBRARY)

$(LIBRARY): $(OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(OBJS) $(LIBS)

.cpp.o:
	@echo "CXX $<"; $(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)

clean:
	@echo "CLEAN"; rm -f $(LIBRARY) $(OBJS) *.d

