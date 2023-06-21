set(SOURCES
    AccessibilityTreeModel.cpp
    DOMTreeModel.cpp
    OutOfProcessWebView.cpp
    RequestServerAdapter.cpp
    StylePropertiesModel.cpp
    ViewImplementation.cpp
    WebContentClient.cpp
    WebSocketClientAdapter.cpp
)

set(GENERATED_SOURCES
    ../../Services/RequestServer/RequestClientEndpoint.h
    ../../Services/RequestServer/RequestServerEndpoint.h
    ../../Services/WebContent/WebContentClientEndpoint.h
    ../../Services/WebContent/WebContentServerEndpoint.h
)

serenity_lib(LibWebView webview)
target_link_libraries(LibWebView PRIVATE LibCore LibFileSystemAccessClient LibGfx LibGUI LibIPC LibProtocol LibWeb)
