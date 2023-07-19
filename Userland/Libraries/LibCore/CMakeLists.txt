set(SOURCES
    AnonymousBuffer.cpp
    ArgsParser.cpp
    Command.cpp
    ConfigFile.cpp
    DateTime.cpp
    Directory.cpp
    DirectoryEntry.cpp
    DirIterator.cpp
    ElapsedTimer.cpp
    Event.cpp
    EventLoop.cpp
    EventLoopImplementation.cpp
    EventLoopImplementationUnix.cpp
    File.cpp
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
    Socket.cpp
    SOCKSProxyClient.cpp
    StandardPaths.cpp
    System.cpp
    SystemServerTakeover.cpp
    TCPServer.cpp
    ThreadEventQueue.cpp
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
    )
endif()
if (NOT WIN32 AND NOT EMSCRIPTEN)
    list(APPEND SOURCES LocalServer.cpp)
endif()

# FIXME: Implement Core::FileWatcher for macOS, *BSD, and Windows.
if (SERENITYOS)
    list(APPEND SOURCES FileWatcherSerenity.cpp)
elseif (LINUX AND NOT EMSCRIPTEN)
    list(APPEND SOURCES FileWatcherLinux.cpp)
elseif (APPLE)
    list(APPEND SOURCES FileWatcherMacOS.mm)
else()
    list(APPEND SOURCES FileWatcherUnimplemented.cpp)
endif()

serenity_lib(LibCore core)
target_link_libraries(LibCore PRIVATE LibCrypt LibSystem)

if (APPLE)
    target_link_libraries(LibCore PUBLIC "-framework CoreFoundation")
    target_link_libraries(LibCore PUBLIC "-framework CoreServices")
    target_link_libraries(LibCore PUBLIC "-framework Foundation")
endif()
