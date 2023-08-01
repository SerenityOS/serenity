set(SOURCES
    Request.cpp
    RequestClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/RequestServer/RequestClientEndpoint.h
    ../../Services/RequestServer/RequestServerEndpoint.h
)

if (SERENITYOS)
    list(APPEND SOURCES
        WebSocket.cpp
        WebSocketClient.cpp
    )
    list(APPEND GENERATED_SOURCES 
        ../../Services/WebSocket/WebSocketClientEndpoint.h
        ../../Services/WebSocket/WebSocketServerEndpoint.h
    )
endif()

serenity_lib(LibProtocol protocol)
target_link_libraries(LibProtocol PRIVATE LibCore LibIPC)
