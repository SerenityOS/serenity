compile_gml(TextEditorWindow.gml TextEditorWindowGML.h text_editor_window_gml)

set(SOURCES
    main.cpp
    TextEditorWidget.cpp
    TextEditorWindowGML.h
)

serenity_app(TextEditor ICON app-text-editor)
target_link_libraries(TextEditor LibWeb LibMarkdown LibGUI LibShell LibRegex LibDesktop LibCpp LibJS)
