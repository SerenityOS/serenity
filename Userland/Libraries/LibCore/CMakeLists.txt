set(SOURCES
    AnonymousBuffer.cpp
    ArgsParser.cpp
    ConfigFile.cpp
    Command.cpp
    DateTime.cpp
    Directory.cpp
    DirIterator.cpp
    ElapsedTimer.cpp
    Event.cpp
    EventLoop.cpp
    FileWatcher.cpp
    File.cpp
    FilePermissionsMask.cpp
    GetPassword.cpp
    Group.cpp
    IODevice.cpp
    LocalServer.cpp
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
    Stream.cpp
    StandardPaths.cpp
    System.cpp
    SystemServerTakeover.cpp
    TCPServer.cpp
    TempFile.cpp
    Timer.cpp
    UDPServer.cpp
    Version.cpp
)
if (NOT ANDROID)
    list(APPEND SOURCES Account.cpp)
endif()

serenity_lib(LibCore core)
target_link_libraries(LibCore LibC LibCrypt)
