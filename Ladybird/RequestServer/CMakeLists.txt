set(REQUESTSERVER_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/RequestServer)

set(REQUESTSERVER_SOURCES
    ${REQUESTSERVER_SOURCE_DIR}/ConnectionFromClient.cpp
    ${REQUESTSERVER_SOURCE_DIR}/ConnectionCache.cpp
    ${REQUESTSERVER_SOURCE_DIR}/Request.cpp
    ${REQUESTSERVER_SOURCE_DIR}/GeminiRequest.cpp
    ${REQUESTSERVER_SOURCE_DIR}/GeminiProtocol.cpp
    ${REQUESTSERVER_SOURCE_DIR}/HttpRequest.cpp
    ${REQUESTSERVER_SOURCE_DIR}/HttpProtocol.cpp
    ${REQUESTSERVER_SOURCE_DIR}/HttpsRequest.cpp
    ${REQUESTSERVER_SOURCE_DIR}/HttpsProtocol.cpp
    ${REQUESTSERVER_SOURCE_DIR}/Protocol.cpp
    main.cpp
)

add_executable(RequestServer ${REQUESTSERVER_SOURCES})

target_include_directories(RequestServer PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(RequestServer PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(RequestServer PRIVATE LibCore LibMain LibCrypto LibFileSystem LibGemini LibHTTP LibIPC LibMain LibTLS LibWebView)
if (ANDROID)
    link_android_libs(RequestServer)
endif()
if (${CMAKE_SYSTEM_NAME} MATCHES "SunOS")
    # Solaris has socket and networking related functions in two extra libraries
    target_link_libraries(RequestServer PRIVATE nsl socket)
endif()
