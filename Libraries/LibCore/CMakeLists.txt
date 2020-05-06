set(SOURCES
    ArgsParser.cpp
    ConfigFile.cpp
    DateTime.cpp
    DirIterator.cpp
    ElapsedTimer.cpp
    Event.cpp
    EventLoop.cpp
    File.cpp
    Gzip.cpp
    IODevice.cpp
    LocalServer.cpp
    LocalSocket.cpp
    MimeData.cpp
    NetworkJob.cpp
    NetworkResponse.cpp
    Notifier.cpp
    Object.cpp
    ProcessStatisticsReader.cpp
    puff.c
    SocketAddress.cpp
    Socket.cpp
    StandardPaths.cpp
    TCPServer.cpp
    TCPSocket.cpp
    Timer.cpp
    UDPServer.cpp
    UDPSocket.cpp
)

serenity_lib(LibCore core)
target_link_libraries(LibCore LibC)
