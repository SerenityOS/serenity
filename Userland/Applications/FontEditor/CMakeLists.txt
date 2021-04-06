include_directories(${CMAKE_CURRENT_BINARY_DIR})
compile_gml(FontEditorWindow.gml FontEditorWindowGML.h font_editor_window_gml)

set(SOURCES
    FontEditor.cpp
    FontEditorWindowGML.h
    GlyphEditorWidget.cpp
    GlyphMapWidget.cpp
    main.cpp
)

serenity_app(FontEditor ICON app-font-editor)
target_link_libraries(FontEditor LibGUI LibDesktop LibGfx)
