set(SOURCES
    main.cpp
    TextEditorWidget.cpp
    MainWindowUI.h
)

add_custom_command(
    OUTPUT MainWindowUI.h
    DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/MainWindow.json
    COMMAND ${write_if_different} MainWindowUI.h ${CMAKE_SOURCE_DIR}/Meta/text-to-cpp-string.sh main_window_ui_json ${CMAKE_CURRENT_SOURCE_DIR}/MainWindow.json
)

serenity_bin(TextEditor)
target_link_libraries(TextEditor LibWeb LibMarkdown LibGUI LibDesktop)
