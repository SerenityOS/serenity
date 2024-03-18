serenity_component(
    HexEditor
    RECOMMENDED
    TARGETS HexEditor
)

compile_gml(EditAnnotationWidget.gml EditAnnotationWidgetGML.cpp)
compile_gml(FindWidget.gml FindWidgetGML.cpp)
compile_gml(GoToOffsetWidget.gml GoToOffsetWidgetGML.cpp)
compile_gml(HexEditorWidget.gml HexEditorWidgetGML.cpp)

set(SOURCES
    AnnotationsModel.cpp
    EditAnnotationDialog.cpp
    EditAnnotationWidgetGML.cpp
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
target_link_libraries(HexEditor PRIVATE LibCore LibGfx LibGUI LibConfig LibDesktop LibFileSystemAccessClient LibMain LibTextCodec LibURL)
