set(SOURCES
    AccessibilityTreeModel.cpp
    AriaPropertiesStateModel.cpp
    DOMTreeModel.cpp
    OutOfProcessWebView.cpp
    RequestServerAdapter.cpp
    StylePropertiesModel.cpp
    ViewImplementation.cpp
    WebContentClient.cpp
    WebSocketClientAdapter.cpp
)

embed_as_string_view(
    "NativeStyleSheetSource.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/Native.css"
    "NativeStyleSheetSource.cpp"
    "native_stylesheet_source"
    NAMESPACE "WebView"
)

set(GENERATED_SOURCES
    ../../Services/RequestServer/RequestClientEndpoint.h
    ../../Services/RequestServer/RequestServerEndpoint.h
    ../../Services/WebContent/WebContentClientEndpoint.h
    ../../Services/WebContent/WebContentServerEndpoint.h
    NativeStyleSheetSource.cpp
)

serenity_lib(LibWebView webview)
target_link_libraries(LibWebView PRIVATE LibCore LibFileSystemAccessClient LibGfx LibGUI LibIPC LibProtocol LibWeb)
