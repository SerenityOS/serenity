set(SOURCES
    Account.cpp
    AnonymousBuffer.cpp
    ArgsParser.cpp
    ConfigFile.cpp
    Command.cpp
    DateTime.cpp
    DirIterator.cpp
    ElapsedTimer.cpp
    Event.cpp
    EventLoop.cpp
    FileWatcher.cpp
    File.cpp
    GetPassword.cpp
    IODevice.cpp
    LocalServer.cpp
    LocalSocket.cpp
    MimeData.cpp
    NetworkJob.cpp
    NetworkResponse.cpp
    Notifier.cpp
    Object.cpp
    ProcessStatisticsReader.cpp
    Property.cpp
    Socket.cpp
    StandardPaths.cpp
    TCPServer.cpp
    TCPSocket.cpp
    Timer.cpp
    UDPServer.cpp
    UDPSocket.cpp
)

serenity_lib(LibCore core)
target_link_libraries(LibCore LibC LibCrypt)
