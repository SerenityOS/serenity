serenity_component(
    HexEditor
    RECOMMENDED
    TARGETS HexEditor
)

compile_gml(HexEditorWindow.gml HexEditorWindowGML.h hex_editor_window_gml)
compile_gml(GoToOffsetDialog.gml GoToOffsetDialogGML.h go_to_offset_dialog_gml)
compile_gml(FindDialog.gml FindDialogGML.h find_dialog_gml)

set(SOURCES
    HexEditor.cpp
    HexEditorWidget.cpp
    HexDocument.cpp
    FindDialog.cpp
    GoToOffsetDialog.cpp
    main.cpp
    FindDialogGML.h
    GoToOffsetDialogGML.h
    HexEditorWindowGML.h
)

serenity_app(HexEditor ICON app-hex-editor)
target_link_libraries(HexEditor LibGUI LibConfig LibFileSystemAccessClient LibMain)
