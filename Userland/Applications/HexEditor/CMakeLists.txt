set(SOURCES
    HexEditor.cpp
    HexEditorWidget.cpp
    FindDialog.cpp
    main.cpp
)

serenity_app(HexEditor ICON app-hexeditor)
target_link_libraries(HexEditor LibGUI)
