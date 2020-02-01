set(SOURCES
    main.cpp
    WSButton.cpp
    WSClientConnection.cpp
    WSClipboard.cpp
    WSCompositor.cpp
    WSCursor.cpp
    WSEventLoop.cpp
    WSMenuBar.cpp
    WSMenu.cpp
    WSMenuItem.cpp
    WSMenuManager.cpp
    WSScreen.cpp
    WSWindow.cpp
    WSWindowFrame.cpp
    WSWindowManager.cpp
    WSWindowSwitcher.cpp
)

add_custom_command(
    OUTPUT  WindowServerEndpoint.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../../DevTools/IPCCompiler/IPCCompiler ${CMAKE_CURRENT_SOURCE_DIR}/WindowServer.ipc > ${CMAKE_CURRENT_BINARY_DIR}/WindowServerEndpoint.h
    DEPENDS IPCCompiler WindowServer.ipc
)

add_custom_command(
    OUTPUT  WindowClientEndpoint.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../../DevTools/IPCCompiler/IPCCompiler ${CMAKE_CURRENT_SOURCE_DIR}/WindowClient.ipc > ${CMAKE_CURRENT_BINARY_DIR}/WindowClientEndpoint.h
    DEPENDS IPCCompiler WindowClient.ipc
)

set(SOURCES ${SOURCES} ${CMAKE_CURRENT_BINARY_DIR}/WindowClientEndpoint.h ${CMAKE_CURRENT_BINARY_DIR}/WindowServerEndpoint.h)

serenity_app(WindowServer)
add_dependencies(WindowServer IPCCompiler)
target_include_directories(WindowServer PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/..)
target_include_directories(WindowServer PRIVATE ..)
target_link_libraries(WindowServer LibIPC LibThread LibDraw LibCore)

target_link_directories(WindowServer PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/../../Libraries/LibC)
target_link_directories(WindowServer PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/../../Libraries/LibM)
