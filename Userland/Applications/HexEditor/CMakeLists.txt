serenity_component(
    HexEditor
    RECOMMENDED
    TARGETS HexEditor
)

compile_gml(HexEditorWidget.gml HexEditorWidgetGML.cpp)
compile_gml(GoToOffsetWidget.gml GoToOffsetWidgetGML.cpp)
compile_gml(FindWidget.gml FindWidgetGML.cpp)

set(SOURCES
    FindDialog.cpp
    FindWidgetGML.cpp
    GoToOffsetDialog.cpp
    GoToOffsetWidgetGML.cpp
    HexDocument.cpp
    HexEditor.cpp
    HexEditorWidgetGML.cpp
    HexEditorWidget.cpp
    main.cpp
)

serenity_app(HexEditor ICON app-hex-editor)
target_link_libraries(HexEditor PRIVATE LibCore LibGfx LibGUI LibConfig LibDesktop LibFileSystemAccessClient LibMain LibTextCodec)
