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
    ImageCodecPluginSerenity.cpp
    PageHost.cpp
    WebContentConsoleClient.cpp
    main.cpp
)

set(GENERATED_SOURCES
    WebContentClientEndpoint.h
    WebContentServerEndpoint.h
)

serenity_bin(WebContent)
target_link_libraries(WebContent PRIVATE LibCore LibIPC LibGfx LibImageDecoderClient LibJS LibWebView LibWeb LibLocale LibMain)
link_with_locale_data(WebContent)
