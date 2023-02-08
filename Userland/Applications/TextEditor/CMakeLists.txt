serenity_component(
    TextEditor
    RECOMMENDED
    TARGETS TextEditor
    DEPENDS ImageDecoder RequestServer WebContent FileSystemAccessServer
)

stringify_gml(TextEditorWindow.gml TextEditorWindowGML.h text_editor_window_gml)

set(SOURCES
    FileArgument.cpp
    MainWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    TextEditorWindowGML.h
)

serenity_app(TextEditor ICON app-text-editor)
target_link_libraries(TextEditor PRIVATE LibCore LibWebView LibWeb LibMarkdown LibGfx LibGUI LibShell LibRegex LibDesktop LibCpp LibCMake LibJS LibSQL LibSyntax LibFileSystemAccessClient LibConfig LibMain)
