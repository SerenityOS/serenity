serenity_component(
    ThemeEditor
    TARGETS ThemeEditor
)

compile_gml(ThemeEditor.gml ThemeEditorGML.h theme_editor_gml)

set(SOURCES
    PreviewWidget.cpp
    main.cpp
    ThemeEditorGML.h
)

serenity_app(ThemeEditor ICON app-theme-editor)
target_link_libraries(ThemeEditor LibGUI LibFileSystemAccessClient LibMain)
