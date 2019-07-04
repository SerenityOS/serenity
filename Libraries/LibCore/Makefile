include ../../Makefile.common

OBJS = \
    CArgsParser.o \
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
    CEvent.o \
    CProcessStatisticsReader.o \
    CDirIterator.o \
    CUserInfo.o

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

install: $(LIBRARY)
	mkdir -p ../../Root/usr/include/LibCore
	mkdir -p ../../Root/usr/include/AK
	mkdir -p ../../Root/usr/lib
	# Copy headers
	rsync -r -a --include '*/' --include '*.h' --exclude '*' . ../../Root/usr/include/LibCore
	rsync -r -a --include '*/' --include '*.h' --exclude '*' ../../AK/ ../../Root/usr/include/AK
	# Install the library
	cp $(LIBRARY) ../../Root/usr/lib
