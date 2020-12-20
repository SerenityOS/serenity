compile_gml(MainWindow.gml MainWindowGML.h main_window_gml)

set(SOURCES
    main.cpp
    TextEditorWidget.cpp
    MainWindowGML.h
)

serenity_app(TextEditor ICON app-text-editor)
target_link_libraries(TextEditor LibWeb LibMarkdown LibGUI LibShell LibRegex LibDesktop)
