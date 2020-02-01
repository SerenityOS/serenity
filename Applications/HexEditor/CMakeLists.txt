set(SOURCES
    main.cpp
    HexEditor.cpp
    HexEditorWidget.cpp
)

serenity_app(HexEditor)
target_link_libraries(HexEditor LibGUI)
