compile_gml(HexEditorWindow.gml HexEditorWindowGML.h hex_editor_window_gml)

set(SOURCES
    HexEditor.cpp
    HexEditorWidget.cpp
    FindDialog.cpp
    main.cpp
    HexEditorWindowGML.h
)

serenity_app(HexEditor ICON app-hex-editor)
target_link_libraries(HexEditor LibGUI)
