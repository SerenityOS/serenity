set(SOURCES
    AnonymousBuffer.cpp
    ArgsParser.cpp
    Command.cpp
    ConfigFile.cpp
    DateTime.cpp
    Directory.cpp
    DirIterator.cpp
    ElapsedTimer.cpp
    Event.cpp
    EventLoop.cpp
    File.cpp
    FileWatcher.cpp
    IODevice.cpp
    LockFile.cpp
    MappedFile.cpp
    MimeData.cpp
    NetworkJob.cpp
    Notifier.cpp
    Object.cpp
    Process.cpp
    ProcessStatisticsReader.cpp
    Property.cpp
    SecretString.cpp
    SessionManagement.cpp
    SOCKSProxyClient.cpp
    StandardPaths.cpp
    Stream.cpp
    System.cpp
    SystemServerTakeover.cpp
    TCPServer.cpp
    TempFile.cpp
    Timer.cpp
    UDPServer.cpp
    Version.cpp
)
if (NOT ANDROID AND NOT WIN32)
    list(APPEND SOURCES
        Account.cpp
        FilePermissionsMask.cpp
        GetPassword.cpp
        Group.cpp
        LocalServer.cpp
    )
endif()

serenity_lib(LibCore core)
target_link_libraries(LibCore LibC LibCrypt LibSystem)
