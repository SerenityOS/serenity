# These are the minimal set of sources needed to build the code generators. We separate them to allow
# LibCore to depend on generated sources.
set(SOURCES
    ArgsParser.cpp
    ConfigFile.cpp
    Directory.cpp
    DirectoryEntry.cpp
    DirIterator.cpp
    Environment.cpp
    File.cpp
    SessionManagement.cpp
    StandardPaths.cpp
    System.cpp
    Version.cpp
)

serenity_lib(LibCoreMinimal coreminimal)
target_link_libraries(LibCoreMinimal PRIVATE LibSystem)

if (NOT BUILD_LAGOM AND NOT SERENITYOS)
    return()
endif()

if (SERENITYOS)
    add_library(DynamicLoader_LibCoreArgsParser 
        ArgsParser.cpp
        Version.cpp)
    target_link_libraries(DynamicLoader_LibCoreArgsParser PUBLIC DynamicLoader_CompileOptions)
endif()

# These are the minimal set of sources needed to build LibLine and LibShellMinimal. We separate them to allow
# LibLine and LibShellMinimal to build without LibURL being included.
set(SOURCES
    DateTime.cpp
    ElapsedTimer.cpp
    Event.cpp
    EventLoop.cpp
    EventLoopImplementation.cpp
    EventLoopImplementationUnix.cpp
    EventReceiver.cpp
    ThreadEventQueue.cpp
    Timer.cpp
    Notifier.cpp
)

serenity_lib(LibCoreBasic corebasic)
target_link_libraries(LibCoreBasic PRIVATE LibCoreMinimal LibTimeZone)

set(SOURCES
    AnonymousBuffer.cpp
    Command.cpp
    LockFile.cpp
    MappedFile.cpp
    MimeData.cpp
    NetworkJob.cpp
    Process.cpp
    ProcessStatisticsReader.cpp
    Resource.cpp
    ResourceImplementation.cpp
    ResourceImplementationFile.cpp
    SecretString.cpp
    Socket.cpp
    SOCKSProxyClient.cpp
    SystemServerTakeover.cpp
    TCPServer.cpp
    UDPServer.cpp
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

# FIXME: Implement Core::FileWatcher for *BSD and Windows.
if (SERENITYOS)
    list(APPEND SOURCES
        FileWatcherSerenity.cpp
        Platform/ProcessStatisticsSerenity.cpp
    )
elseif (LINUX AND NOT EMSCRIPTEN)
    list(APPEND SOURCES
        FileWatcherLinux.cpp
        Platform/ProcessStatisticsLinux.cpp
    )
elseif (APPLE AND NOT IOS)
    list(APPEND SOURCES
        FileWatcherMacOS.mm
        Platform/ProcessStatisticsMach.cpp
    )
else()
    list(APPEND SOURCES
        FileWatcherUnimplemented.cpp
        Platform/ProcessStatisticsUnimplemented.cpp
    )
endif()

if (APPLE OR CMAKE_SYSTEM_NAME STREQUAL "GNU")
    list(APPEND SOURCES MachPort.cpp)
endif()

serenity_lib(LibCore core)
target_link_libraries(LibCore PRIVATE LibCrypt LibSystem LibTimeZone LibURL)
target_link_libraries(LibCore PUBLIC LibCoreMinimal LibCoreBasic)

if (APPLE)
    target_link_libraries(LibCore PUBLIC "-framework CoreFoundation")
    target_link_libraries(LibCore PUBLIC "-framework CoreServices")
    target_link_libraries(LibCore PUBLIC "-framework Foundation")
endif()

if (ANDROID)
    target_link_libraries(LibCore PRIVATE log)
endif()
