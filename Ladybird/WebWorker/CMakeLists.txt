set(WEBWORKER_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/WebWorker)

set(CMAKE_AUTOMOC OFF)
set(CMAKE_AUTORCC OFF)
set(CMAKE_AUTOUIC OFF)

set(WEBWORKER_SOURCES
    "${WEBWORKER_SOURCE_DIR}/ConnectionFromClient.cpp"
    "${WEBWORKER_SOURCE_DIR}/DedicatedWorkerHost.cpp"
    "${WEBWORKER_SOURCE_DIR}/PageHost.cpp"
    ../HelperProcess.cpp
    ../Utilities.cpp
)

# FIXME: Add Android service

add_library(webworker_svc STATIC ${WEBWORKER_SOURCES})

target_include_directories(webworker_svc PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(webworker_svc PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(webworker_svc PUBLIC LibCore LibFileSystem LibGfx LibIPC LibJS LibProtocol LibWeb LibWebView LibLocale LibImageDecoderClient LibMain)

add_executable(WebWorker main.cpp)
target_link_libraries(WebWorker PRIVATE webworker_svc)
set_helper_process_properties(WebWorker)
