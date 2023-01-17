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
    IODevice.cpp
    LockFile.cpp
    MappedFile.cpp
    MemoryStream.cpp
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
if (NOT ANDROID AND NOT WIN32 AND NOT EMSCRIPTEN)
    list(APPEND SOURCES
        Account.cpp
        FilePermissionsMask.cpp
        GetPassword.cpp
        Group.cpp
        LocalServer.cpp
    )
endif()

# FIXME: Implement Core::FileWatcher for Linux, macOS, *BSD, and Windows.
if (SERENITYOS)
    list(APPEND SOURCES FileWatcherSerenity.cpp)
else()
    list(APPEND SOURCES FileWatcherUnimplemented.cpp)
endif()

serenity_lib(LibCore core)
target_link_libraries(LibCore PRIVATE LibCrypt LibSystem)
