set(SOURCES
    AnonymousBuffer.cpp
    ArgsParser.cpp
    ConfigFile.cpp
    DateTime.cpp
    Directory.cpp
    ElapsedTimer.cpp
    Event.cpp
    EventLoop.cpp
    EventLoopImplementation.cpp
    EventReceiver.cpp
    File.cpp
    MappedFile.cpp
    MimeData.cpp
    NetworkJob.cpp
    Notifier.cpp
    Resource.cpp
    ResourceImplementation.cpp
    ResourceImplementationFile.cpp
    SecretString.cpp
    SessionManagement.cpp
    Socket.cpp
    SOCKSProxyClient.cpp
    StandardPaths.cpp
    ThreadEventQueue.cpp
    Timer.cpp
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

if(WIN32)
    list(APPEND SOURCES 
        DirectoryEntryWindows.cpp 
        DirIteratorWindows.cpp
        EventLoopImplementationWindows.cpp
        SystemWindows.cpp
    )
else()
    list(APPEND SOURCES 
        Command.cpp
        DirectoryEntry.cpp 
        DirIterator.cpp
        EventLoopImplementationUnix.cpp
        LockFile.cpp
        Process.cpp
        ProcessStatisticsReader.cpp
        System.cpp
        SystemServerTakeover.cpp
        TCPServer.cpp
        UDPServer.cpp
    )
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
elseif (WIN32)
    target_link_libraries(LibCore PUBLIC ws2_32.lib)
endif()

if (ANDROID)
    target_link_libraries(LibCore PRIVATE log)
endif()
