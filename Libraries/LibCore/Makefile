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

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibCore/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibCore/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
