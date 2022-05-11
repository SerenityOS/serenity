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

set(SOURCES
    main.cpp
    MainWidget.cpp
    PreviewWidget.cpp
    AlignmentPropertyGML.h
    ColorPropertyGML.h
    FlagPropertyGML.h
    MetricPropertyGML.h
    PathPropertyGML.h
    ThemeEditorGML.h
)

serenity_app(ThemeEditor ICON app-theme-editor)
target_link_libraries(ThemeEditor LibGUI LibFileSystemAccessClient LibMain)
