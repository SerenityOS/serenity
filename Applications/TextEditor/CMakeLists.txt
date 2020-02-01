set(SOURCES
    main.cpp
    TextEditorWidget.cpp
)

serenity_app(TextEditor)
target_link_libraries(TextEditor LibGUI)
