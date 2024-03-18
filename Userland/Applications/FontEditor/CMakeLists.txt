serenity_component(
    FontEditor
    RECOMMENDED
    TARGETS FontEditor
)

include_directories(${CMAKE_CURRENT_BINARY_DIR})
stringify_gml(FontEditorWindow.gml FontEditorWindowGML.h font_editor_window_gml)
stringify_gml(FontPreviewWindow.gml FontPreviewWindowGML.h font_preview_window_gml)
stringify_gml(NewFontDialogPage1.gml NewFontDialogPage1GML.h new_font_dialog_page_1_gml)
stringify_gml(NewFontDialogPage2.gml NewFontDialogPage2GML.h new_font_dialog_page_2_gml)

set(SOURCES
    MainWidget.cpp
    GlyphEditorWidget.cpp
    main.cpp
    NewFontDialog.cpp
)

set(GENERATED_SOURCES
    FontEditorWindowGML.h
    FontPreviewWindowGML.h
    NewFontDialogPage1GML.h
    NewFontDialogPage2GML.h
)

serenity_app(FontEditor ICON app-font-editor)
target_link_libraries(FontEditor PRIVATE LibConfig LibCore LibFileSystemAccessClient LibGUI LibDesktop LibGfx LibMain LibUnicode LibURL)
