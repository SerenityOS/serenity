compile_json_gui(MainWindow.json MainWindowUI.h main_window_ui_json)

set(SOURCES
    main.cpp
    TextEditorWidget.cpp
    MainWindowUI.h
)

serenity_bin(TextEditor)
target_link_libraries(TextEditor LibWeb LibMarkdown LibGUI LibShell LibDesktop)
