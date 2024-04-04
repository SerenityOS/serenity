include(${SerenityOS_SOURCE_DIR}/Meta/CMake/public_suffix.cmake)

set(SOURCES
    Attribute.cpp
    CookieJar.cpp
    Database.cpp
    History.cpp
    InspectorClient.cpp
    ProcessHandle.cpp
    ProcessManager.cpp
    RequestServerAdapter.cpp
    SearchEngine.cpp
    SocketPair.cpp
    SourceHighlighter.cpp
    URL.cpp
    UserAgent.cpp
    ViewImplementation.cpp
    WebContentClient.cpp
    WebSocketClientAdapter.cpp
    ${PUBLIC_SUFFIX_SOURCES}
)

set(GENERATED_SOURCES ${CURRENT_LIB_GENERATED})

if (SERENITYOS)
    list(APPEND SOURCES
        OutOfProcessWebView.cpp
        Platform/ProcessStatisticsSerenity.cpp
    )
elseif(LINUX AND NOT ANDROID)
    list(APPEND SOURCES
        Platform/ProcessStatisticsLinux.cpp
    )
elseif(APPLE)
    list(APPEND SOURCES
        Platform/ProcessStatisticsMach.cpp
    )
else()
    list(APPEND SOURCES
        Platform/ProcessStatisticsNoop.cpp
    )
endif()

embed_as_string_view(
    "NativeStyleSheetSource.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/Native.css"
    "NativeStyleSheetSource.cpp"
    "native_stylesheet_source"
    NAMESPACE "WebView"
)

set(GENERATED_SOURCES
    ${GENERATED_SOURCES}
    ../../Services/RequestServer/RequestClientEndpoint.h
    ../../Services/RequestServer/RequestServerEndpoint.h
    ../../Services/WebContent/WebContentClientEndpoint.h
    ../../Services/WebContent/WebContentServerEndpoint.h
    ../../Services/WebContent/WebDriverClientEndpoint.h
    ../../Services/WebContent/WebDriverServerEndpoint.h
    NativeStyleSheetSource.cpp
)

serenity_lib(LibWebView webview)
target_link_libraries(LibWebView PRIVATE LibCore LibFileSystem LibGfx LibIPC LibProtocol LibJS LibWeb LibSQL LibUnicode LibURL)
target_compile_definitions(LibWebView PRIVATE ENABLE_PUBLIC_SUFFIX=$<BOOL:${ENABLE_PUBLIC_SUFFIX_DOWNLOAD}>)

if (SERENITYOS)
    target_link_libraries(LibWebView PRIVATE LibFileSystemAccessClient LibGUI)
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
