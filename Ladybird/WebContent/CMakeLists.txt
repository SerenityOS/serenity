set(WEBCONTENT_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/WebContent/)

set(WEBCONTENT_SOURCES
    ${WEBCONTENT_SOURCE_DIR}/ConnectionFromClient.cpp
    ${WEBCONTENT_SOURCE_DIR}/ConsoleGlobalEnvironmentExtensions.cpp
    ${WEBCONTENT_SOURCE_DIR}/PageHost.cpp
    ${WEBCONTENT_SOURCE_DIR}/WebContentConsoleClient.cpp
    ${WEBCONTENT_SOURCE_DIR}/WebDriverConnection.cpp
    ../FontPlugin.cpp
    ../HelperProcess.cpp
    ../ImageCodecPlugin.cpp
    ../Utilities.cpp
    main.cpp
)

if (ENABLE_QT)
    qt_add_executable(WebContent ${WEBCONTENT_SOURCES})
    target_sources(WebContent PRIVATE
        ../Qt/AudioCodecPluginQt.cpp
        ../Qt/AudioThread.cpp
        ../Qt/EventLoopImplementationQt.cpp
        ../Qt/EventLoopImplementationQtEventTarget.cpp
        ../Qt/RequestManagerQt.cpp
        ../Qt/StringUtils.cpp
        ../Qt/WebSocketClientManagerQt.cpp
        ../Qt/WebSocketQt.cpp
        ../Qt/WebSocketImplQt.cpp
    )
    target_link_libraries(WebContent PRIVATE Qt::Core Qt::Network Qt::Multimedia)
    target_compile_definitions(WebContent PRIVATE HAVE_QT=1)
else()
    add_executable(WebContent ${WEBCONTENT_SOURCES})
endif()

target_include_directories(WebContent PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(WebContent PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(WebContent PRIVATE LibAudio LibCore LibFileSystem LibGfx LibIPC LibJS LibMain LibWeb LibWebSocket LibProtocol LibSQL LibWebView)
if (ANDROID)
    link_android_libs(WebContent)
endif()

if (HAVE_PULSEAUDIO)
    target_compile_definitions(WebContent PRIVATE HAVE_PULSEAUDIO=1)
endif()
