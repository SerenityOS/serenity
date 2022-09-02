serenity_component(
    WebContent
    TARGETS WebContent
    DEPENDS ImageDecoder RequestServer WebSocket
)

compile_ipc(WebContentServer.ipc WebContentServerEndpoint.h)
compile_ipc(WebContentClient.ipc WebContentClientEndpoint.h)

set(SOURCES
    ConnectionFromClient.cpp
    ConsoleGlobalObject.cpp
    main.cpp
    PageHost.cpp
    WebContentConsoleClient.cpp
    WebContentServerEndpoint.h
    WebContentClientEndpoint.h
)

serenity_bin(WebContent)
target_link_libraries(WebContent LibCore LibIPC LibGfx LibWebView LibWeb LibMain)
link_with_locale_data(WebContent)
