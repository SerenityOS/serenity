set(WEBWORKER_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/WebWorker)

set(WEBWORKER_SOURCES
    "${WEBWORKER_SOURCE_DIR}/ConnectionFromClient.cpp"
    "${WEBWORKER_SOURCE_DIR}/DedicatedWorkerHost.cpp"
    "${WEBWORKER_SOURCE_DIR}/PageHost.cpp"
    ../HelperProcess.cpp
    ../Utilities.cpp
)

add_library(webworker STATIC ${WEBWORKER_SOURCES})

target_include_directories(webworker PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(webworker PRIVATE ${SERENITY_SOURCE_DIR}/Userland/)
target_include_directories(webworker PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(webworker PUBLIC LibCore LibFileSystem LibGfx LibIPC LibJS LibProtocol LibWeb LibWebView LibLocale LibImageDecoderClient LibMain LibSQL LibURL)

if (ENABLE_QT)
    qt_add_executable(WebWorker
            ../Qt/EventLoopImplementationQt.cpp
            ../Qt/EventLoopImplementationQtEventTarget.cpp
            ../Qt/StringUtils.cpp
            main.cpp
    )
    target_link_libraries(WebWorker PRIVATE Qt::Core)
    target_link_libraries(WebWorker PRIVATE webworker LibWebSocket)
    target_compile_definitions(WebWorker PRIVATE HAVE_QT=1)
    set_target_properties(WebWorker PROPERTIES AUTOMOC ON)

else()
    add_executable(WebWorker main.cpp)
endif()

target_include_directories(WebWorker PRIVATE ${SERENITY_SOURCE_DIR}/Userland/)
target_link_libraries(WebWorker PRIVATE webworker)
