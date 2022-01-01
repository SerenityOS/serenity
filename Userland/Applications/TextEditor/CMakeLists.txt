serenity_component(
    TextEditor
    RECOMMENDED
    TARGETS TextEditor
    DEPENDS ImageDecoder RequestServer WebContent FileSystemAccessServer
)

compile_gml(TextEditorWindow.gml TextEditorWindowGML.h text_editor_window_gml)

set(SOURCES
    main.cpp
    FileArgument.cpp
    MainWidget.cpp
    TextEditorWindowGML.h
)

serenity_app(TextEditor ICON app-text-editor)
target_link_libraries(TextEditor LibWeb LibMarkdown LibGUI LibShell LibRegex LibDesktop LibCpp LibJS LibSQL LibFileSystemAccessClient LibConfig LibMain)
link_with_unicode_data(TextEditor)
