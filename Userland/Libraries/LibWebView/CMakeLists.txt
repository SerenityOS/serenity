set(SOURCES
    OutOfProcessWebView.cpp
    WebContentClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/RequestServer/RequestClientEndpoint.h
    ../../Services/RequestServer/RequestServerEndpoint.h
    ../../Services/WebContent/WebContentClientEndpoint.h
    ../../Services/WebContent/WebContentServerEndpoint.h
)

serenity_lib(LibWebView webview)
target_link_libraries(LibWebView LibGfx LibGUI LibIPC LibWeb)
