set(WEBCONTENT_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/WebContent/)

set(WEBCONTENT_SOURCES
    ${WEBCONTENT_SOURCE_DIR}/ConnectionFromClient.cpp
    ${WEBCONTENT_SOURCE_DIR}/ConsoleGlobalEnvironmentExtensions.cpp
    ${WEBCONTENT_SOURCE_DIR}/PageHost.cpp
    ${WEBCONTENT_SOURCE_DIR}/WebContentConsoleClient.cpp
    ${WEBCONTENT_SOURCE_DIR}/WebDriverConnection.cpp
    ../AudioCodecPluginLadybird.cpp
    ../AudioThread.cpp
    ../EventLoopImplementationQt.cpp
    ../FontPluginQt.cpp
    ../ImageCodecPluginLadybird.cpp
    ../RequestManagerQt.cpp
    ../Utilities.cpp
    ../WebSocketClientManagerLadybird.cpp
    ../WebSocketLadybird.cpp
    ../WebSocketImplQt.cpp
    main.cpp
)

if (APPLE)
    list(APPEND WEBCONTENT_SOURCES MacOSSetup.mm)
endif()

qt_add_executable(WebContent ${WEBCONTENT_SOURCES})

target_include_directories(WebContent PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(WebContent PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(WebContent PRIVATE Qt::Core Qt::Gui Qt::Network Qt::Multimedia LibAudio LibCore LibFileSystem LibGfx LibIPC LibJS LibMain LibWeb LibWebSocket)
if (ANDROID)
    link_android_libs(WebContent)
endif()
