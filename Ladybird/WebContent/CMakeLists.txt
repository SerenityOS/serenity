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
        main.cpp
    )
    target_link_libraries(WebContent PRIVATE Qt::Core Qt::Network Qt::Multimedia)
    target_compile_definitions(WebContent PRIVATE HAVE_QT=1)
else()
    set(LIB_TYPE STATIC)
    if (ANDROID)
        set(LIB_TYPE SHARED)
    endif()
    add_library(webcontent ${LIB_TYPE} ${WEBCONTENT_SOURCES})
    target_include_directories(webcontent PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
    target_include_directories(webcontent PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
    target_link_libraries(webcontent PRIVATE LibAudio LibCore LibFileSystem LibGfx LibIPC LibJS LibMain LibWeb LibWebSocket LibProtocol LibWebView)
    target_sources(webcontent PUBLIC FILE_SET ladybird TYPE HEADERS
        BASE_DIRS ${SERENITY_SOURCE_DIR}
        FILES ../FontPlugin.h
              ../ImageCodecPlugin.h
    )
    target_sources(webcontent PUBLIC FILE_SET server TYPE HEADERS
        BASE_DIRS ${SERENITY_SOURCE_DIR}/Userland/Services
        FILES ${WEBCONTENT_SOURCE_DIR}/ConnectionFromClient.h
              ${WEBCONTENT_SOURCE_DIR}/ConsoleGlobalEnvironmentExtensions.h
              ${WEBCONTENT_SOURCE_DIR}/Forward.h
              ${WEBCONTENT_SOURCE_DIR}/PageHost.h
              ${WEBCONTENT_SOURCE_DIR}/WebContentConsoleClient.h
              ${WEBCONTENT_SOURCE_DIR}/WebDriverConnection.h
    )

    if (ANDROID)
        target_sources(webcontent PRIVATE
          ../Android/src/main/cpp/WebContentService.cpp
          ../Android/src/main/cpp/ALooperEventLoopImplementation.cpp
          ../Android/src/main/cpp/TimerExecutorService.cpp
        )
        target_link_libraries(webcontent PRIVATE log android)
    endif()

    add_executable(WebContent main.cpp)
    target_link_libraries(WebContent PRIVATE webcontent)
endif()

target_include_directories(WebContent PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(WebContent PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(WebContent PRIVATE LibAudio LibCore LibFileSystem LibGfx LibIPC LibJS LibMain LibWeb LibWebSocket LibProtocol LibWebView)

if (HAVE_PULSEAUDIO)
    target_compile_definitions(WebContent PRIVATE HAVE_PULSEAUDIO=1)
    if (TARGET webcontent)
        target_compile_definitions(webcontent PRIVATE HAVE_PULSEAUDIO=1)
    endif()
endif()
