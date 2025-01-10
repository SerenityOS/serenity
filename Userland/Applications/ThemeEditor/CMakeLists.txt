serenity_component(
    ThemeEditor
    TARGETS ThemeEditor
)

compile_gml(ThemeEditor.gml ThemeEditorGML.cpp)
compile_gml(AlignmentProperty.gml AlignmentPropertyGML.cpp)
compile_gml(ColorProperty.gml ColorPropertyGML.cpp)
compile_gml(FlagProperty.gml FlagPropertyGML.cpp)
compile_gml(MetricProperty.gml MetricPropertyGML.cpp)
compile_gml(PathProperty.gml PathPropertyGML.cpp)
compile_gml(PreviewWidget.gml PreviewWidgetGML.cpp)
compile_gml(Previews/WindowPreview.gml Previews/WindowPreviewGML.cpp)
compile_gml(WindowThemeProperty.gml WindowThemePropertyGML.cpp)

set(SOURCES
    MainWidget.cpp
    PreviewWidget.cpp
    main.cpp

    AlignmentPropertyGML.cpp
    ColorPropertyGML.cpp
    FlagPropertyGML.cpp
    MetricPropertyGML.cpp
    PathPropertyGML.cpp
    ThemeEditorGML.cpp
    PreviewWidgetGML.cpp
    WindowThemePropertyGML.cpp
    Previews/WindowPreview.cpp
    Previews/WindowPreviewGML.cpp
)

serenity_app(ThemeEditor ICON app-theme-editor)
target_link_libraries(ThemeEditor PRIVATE LibConfig LibCore LibGfx LibGUI LibFileSystem LibFileSystemAccessClient LibIPC LibMain LibURL)
