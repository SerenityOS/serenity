set(SOURCES
    main.cpp
    PSClientConnection.cpp
    HttpDownload.cpp
    HttpProtocol.cpp
    Protocol.cpp
    Download.cpp
)

add_custom_command(
    OUTPUT  ProtocolServerEndpoint.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../../DevTools/IPCCompiler/IPCCompiler ${CMAKE_CURRENT_SOURCE_DIR}/ProtocolServer.ipc > ${CMAKE_CURRENT_BINARY_DIR}/ProtocolServerEndpoint.h
    DEPENDS IPCCompiler ProtocolServer.ipc
)

add_custom_command(
    OUTPUT  ProtocolClientEndpoint.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../../DevTools/IPCCompiler/IPCCompiler ${CMAKE_CURRENT_SOURCE_DIR}/ProtocolClient.ipc > ${CMAKE_CURRENT_BINARY_DIR}/ProtocolClientEndpoint.h
    DEPENDS IPCCompiler ProtocolClient.ipc
)

set(SOURCES ${SOURCES} ${CMAKE_CURRENT_BINARY_DIR}/ProtocolClientEndpoint.h ${CMAKE_CURRENT_BINARY_DIR}/ProtocolServerEndpoint.h)

serenity_app(ProtocolServer)
add_dependencies(ProtocolServer IPCCompiler)
target_include_directories(ProtocolServer PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/..)
target_include_directories(ProtocolServer PRIVATE ..)
target_link_libraries(ProtocolServer LibIPC LibCore)
