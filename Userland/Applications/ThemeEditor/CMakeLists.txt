serenity_component(
    ThemeEditor
    TARGETS ThemeEditor
)

stringify_gml(ThemeEditor.gml ThemeEditorGML.h theme_editor_gml)
stringify_gml(AlignmentProperty.gml AlignmentPropertyGML.h alignment_property_gml)
stringify_gml(ColorProperty.gml ColorPropertyGML.h color_property_gml)
stringify_gml(FlagProperty.gml FlagPropertyGML.h flag_property_gml)
stringify_gml(MetricProperty.gml MetricPropertyGML.h metric_property_gml)
stringify_gml(PathProperty.gml PathPropertyGML.h path_property_gml)
stringify_gml(WindowThemeProperty.gml WindowThemePropertyGML.h window_theme_property_gml)
stringify_gml(Previews/WindowPreview.gml WindowPreviewGML.h window_preview_gml)

set(SOURCES
    MainWidget.cpp
    PreviewWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    AlignmentPropertyGML.h
    ColorPropertyGML.h
    FlagPropertyGML.h
    MetricPropertyGML.h
    PathPropertyGML.h
    ThemeEditorGML.h
    WindowPreviewGML.h
    WindowThemePropertyGML.h
)

serenity_app(ThemeEditor ICON app-theme-editor)
target_link_libraries(ThemeEditor PRIVATE LibConfig LibCore LibGfx LibGUI LibFileSystem LibFileSystemAccessClient LibIPC LibMain LibURL)
