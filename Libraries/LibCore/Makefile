include ../../Makefile.common

OBJS = \
    CArgsParser.o \
    CIODevice.o \
    CFile.o \
    CSocket.o \
    CLocalSocket.o \
    CLocalServer.o \
    CTCPSocket.o \
    CTCPServer.o \
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
    CEvent.o \
    CProcessStatisticsReader.o \
    CDirIterator.o \
    CUserInfo.o \
    CGzip.o

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
