set(SOURCES
    main.cpp
    ASClientConnection.cpp
    ASEventLoop.cpp
    ASMixer.cpp
)

add_custom_command(
    OUTPUT  AudioServerEndpoint.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../../DevTools/IPCCompiler/IPCCompiler ${CMAKE_CURRENT_SOURCE_DIR}/AudioServer.ipc > ${CMAKE_CURRENT_BINARY_DIR}/AudioServerEndpoint.h
    DEPENDS IPCCompiler AudioServer.ipc
)

add_custom_command(
    OUTPUT  AudioClientEndpoint.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../../DevTools/IPCCompiler/IPCCompiler ${CMAKE_CURRENT_SOURCE_DIR}/AudioClient.ipc > ${CMAKE_CURRENT_BINARY_DIR}/AudioClientEndpoint.h
    DEPENDS IPCCompiler AudioClient.ipc
)

set(SOURCES ${SOURCES} ${CMAKE_CURRENT_BINARY_DIR}/AudioClientEndpoint.h ${CMAKE_CURRENT_BINARY_DIR}/AudioServerEndpoint.h)

serenity_app(AudioServer)
add_dependencies(AudioServer IPCCompiler)
target_include_directories(AudioServer PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/..)
target_include_directories(AudioServer PRIVATE ..)
target_link_libraries(AudioServer LibIPC LibThread LibCore)
