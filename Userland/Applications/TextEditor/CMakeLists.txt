serenity_component(
    TextEditor
    RECOMMENDED
    TARGETS TextEditor
    DEPENDS ImageDecoder RequestServer WebContent FileSystemAccessServer
)

compile_gml(TextEditorWindow.gml TextEditorWindowGML.cpp)

set(SOURCES
    FileArgument.cpp
    MainWidget.cpp
    TextEditorWindowGML.cpp
    main.cpp
)

set(GENERATED_SOURCES
    TextEditorWindowGML.cpp
)

serenity_app(TextEditor ICON app-text-editor)
target_link_libraries(TextEditor PRIVATE LibCore LibWebView LibWeb LibMarkdown LibGemini LibGfx LibGUI LibShell LibRegex LibDesktop LibCpp LibCMake LibJS LibSQL LibSyntax LibFileSystemAccessClient LibConfig LibMain LibURL)
