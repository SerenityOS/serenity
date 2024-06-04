include(accelerated_graphics)
set(WEBCONTENT_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/WebContent/)

set(WEBCONTENT_SOURCES
    ${WEBCONTENT_SOURCE_DIR}/ConnectionFromClient.cpp
    ${WEBCONTENT_SOURCE_DIR}/ConsoleGlobalEnvironmentExtensions.cpp
    ${WEBCONTENT_SOURCE_DIR}/PageClient.cpp
    ${WEBCONTENT_SOURCE_DIR}/PageHost.cpp
    ${WEBCONTENT_SOURCE_DIR}/WebContentConsoleClient.cpp
    ${WEBCONTENT_SOURCE_DIR}/WebDriverConnection.cpp
    ../FontPlugin.cpp
    ../HelperProcess.cpp
    ../ImageCodecPlugin.cpp
    ../Utilities.cpp
)

add_library(webcontent STATIC ${WEBCONTENT_SOURCES})
set_target_properties(webcontent PROPERTIES AUTOMOC OFF AUTORCC OFF AUTOUIC OFF)

target_include_directories(webcontent PUBLIC ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(webcontent PUBLIC ${SERENITY_SOURCE_DIR}/Userland/)
target_include_directories(webcontent PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(webcontent PUBLIC LibAudio LibCore LibFileSystem LibGfx LibIPC LibJS LibMain LibWeb LibWebSocket LibProtocol LibWebView LibImageDecoderClient)

if (HAVE_PULSEAUDIO)
    target_compile_definitions(webcontent PUBLIC HAVE_PULSEAUDIO=1)
endif()

if (HAS_ACCELERATED_GRAPHICS)
    target_compile_definitions(webcontent PUBLIC HAS_ACCELERATED_GRAPHICS)
    target_link_libraries(webcontent PUBLIC LibAccelGfx)
endif()

add_executable(WebContent main.cpp)

target_link_libraries(WebContent PRIVATE webcontent LibSQL LibURL)

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
