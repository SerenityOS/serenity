set(SOURCES
    main.cpp
    TextEditorWidget.cpp
)

serenity_bin(TextEditor)
target_link_libraries(TextEditor LibWeb LibMarkdown LibGUI)
