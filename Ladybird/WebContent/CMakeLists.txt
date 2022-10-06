set(WEBCONTENT_SOURCE_DIR ${SERENITY_SOURCE_DIR}/Userland/Services/WebContent/)

set(WEBCONTENT_SOURCES
    ${WEBCONTENT_SOURCE_DIR}/ConnectionFromClient.cpp    
    ${WEBCONTENT_SOURCE_DIR}/ConsoleGlobalObject.cpp    
    ${WEBCONTENT_SOURCE_DIR}/PageHost.cpp    
    ${WEBCONTENT_SOURCE_DIR}/WebContentConsoleClient.cpp    
    ../EventLoopPluginQt.cpp
    ../FontPluginQt.cpp
    ../ImageCodecPluginLadybird.cpp
    ../RequestManagerQt.cpp
    ../TimerQt.cpp
    ../Utilities.cpp
    ../WebSocketClientManagerLadybird.cpp
    ../WebSocketLadybird.cpp
    main.cpp
)    

qt_add_executable(WebContent ${WEBCONTENT_SOURCES})

target_include_directories(WebContent PRIVATE ${SERENITY_SOURCE_DIR}/Userland/Services/)
target_include_directories(WebContent PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/..)
target_link_libraries(WebContent PRIVATE Qt::Core Qt::Network Qt::Gui LibCore LibIPC LibGfx LibWebView LibWebSocket LibWeb LibMain)
