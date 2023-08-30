set(SOURCES
    AccessibilityTreeModel.cpp
    AriaPropertiesStateModel.cpp
    ConsoleClient.cpp
    DOMTreeModel.cpp
    RequestServerAdapter.cpp
    SourceHighlighter.cpp
    StylePropertiesModel.cpp
    ViewImplementation.cpp
    WebContentClient.cpp
    WebSocketClientAdapter.cpp
)

if (SERENITYOS)
    list(APPEND SOURCES OutOfProcessWebView.cpp)
endif()

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
    ../../Services/WebContent/WebDriverClientEndpoint.h
    ../../Services/WebContent/WebDriverServerEndpoint.h
    ../../Services/WebSocket/WebSocketClientEndpoint.h
    ../../Services/WebSocket/WebSocketServerEndpoint.h
    NativeStyleSheetSource.cpp
)

serenity_lib(LibWebView webview)
target_link_libraries(LibWebView PRIVATE LibCore LibGfx LibGUI LibIPC LibProtocol LibJS LibWeb)

if (SERENITYOS)
    target_link_libraries(LibWebView PRIVATE LibFileSystemAccessClient)
endif()

if (NOT SERENITYOS)
    foreach(header ${GENERATED_SOURCES})
        get_filename_component(extension ${header} EXT)
        if (NOT "${extension}" STREQUAL ".h")
            continue()
        endif()

        get_filename_component(subdirectory ${header} DIRECTORY)
        string(REGEX REPLACE "^\\.\\./\\.\\./" "" subdirectory "${subdirectory}")

        install(FILES "${CMAKE_CURRENT_BINARY_DIR}/${header}" DESTINATION "${CMAKE_INSTALL_INCLUDEDIR}/${subdirectory}")
    endforeach()

    install(FILES "${SERENITY_PROJECT_ROOT}/Userland/Services/RequestServer/ConnectionCache.h" DESTINATION "${CMAKE_INSTALL_INCLUDEDIR}/RequestServer")
endif()
