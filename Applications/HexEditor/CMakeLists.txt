set(SOURCES
    HexEditor.cpp
    HexEditorWidget.cpp
    main.cpp
)

serenity_bin(HexEditor)
target_link_libraries(HexEditor LibGUI)
