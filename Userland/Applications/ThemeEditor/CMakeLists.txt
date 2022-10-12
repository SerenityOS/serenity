serenity_component(
    ThemeEditor
    TARGETS ThemeEditor
)

compile_gml(ThemeEditor.gml ThemeEditorGML.h theme_editor_gml)
compile_gml(AlignmentProperty.gml AlignmentPropertyGML.h alignment_property_gml)
compile_gml(ColorProperty.gml ColorPropertyGML.h color_property_gml)
compile_gml(FlagProperty.gml FlagPropertyGML.h flag_property_gml)
compile_gml(MetricProperty.gml MetricPropertyGML.h metric_property_gml)
compile_gml(PathProperty.gml PathPropertyGML.h path_property_gml)
compile_gml(Previews/WindowPreview.gml WindowPreviewGML.h window_preview_gml)

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
)

serenity_app(ThemeEditor ICON app-theme-editor)
target_link_libraries(ThemeEditor LibGUI LibFileSystemAccessClient LibMain)
