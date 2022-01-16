serenity_component(
    FontEditor
    RECOMMENDED
    TARGETS FontEditor
)

include_directories(${CMAKE_CURRENT_BINARY_DIR})
compile_gml(FontEditorWindow.gml FontEditorWindowGML.h font_editor_window_gml)
compile_gml(NewFontDialogPage1.gml NewFontDialogPage1GML.h new_font_dialog_page_1_gml)
compile_gml(NewFontDialogPage2.gml NewFontDialogPage2GML.h new_font_dialog_page_2_gml)

set(SOURCES
    FontEditor.cpp
    FontEditorWindowGML.h
    GlyphEditorWidget.cpp
    main.cpp
    NewFontDialog.cpp
    NewFontDialogPage1GML.h
    NewFontDialogPage2GML.h
    UndoGlyph.h
)

serenity_app(FontEditor ICON app-font-editor)
target_link_libraries(FontEditor LibGUI LibDesktop LibGfx LibMain)
link_with_unicode_data(FontEditor)
