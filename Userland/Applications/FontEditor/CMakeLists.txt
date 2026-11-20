serenity_component(
    FontEditor
    RECOMMENDED
    TARGETS FontEditor
)

include_directories(${CMAKE_CURRENT_BINARY_DIR})
set(SOURCES
    MainWidget.cpp
    GlyphEditorWidget.cpp
    main.cpp
    NewFontDialog.cpp
)

serenity_app(FontEditor ICON app-font-editor)
target_link_libraries(FontEditor PRIVATE LibConfig LibCore LibFileSystemAccessClient LibGUI LibDesktop LibGfx LibMain LibUnicode LibURL)
