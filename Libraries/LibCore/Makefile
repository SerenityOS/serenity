OBJS = \
    ArgsParser.o \
    DateTime.o \
    IODevice.o \
    File.o \
    Socket.o \
    LocalSocket.o \
    LocalServer.o \
    TCPSocket.o \
    TCPServer.o \
    UdpSocket.o \
    UdpServer.o \
    ElapsedTimer.o \
    Notifier.o \
    HttpRequest.o \
    HttpResponse.o \
    HttpJob.o \
    NetworkJob.o \
    NetworkResponse.o \
    Object.o \
    Timer.o \
    EventLoop.o \
    ConfigFile.o \
    Event.o \
    ProcessStatisticsReader.o \
    DirIterator.o \
    UserInfo.o \
    Gzip.o \
    puff.o

LIBRARY = libcore.a

install:
	mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibCore/
	cp *.h $(SERENITY_BASE_DIR)/Root/usr/include/LibCore/
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common
