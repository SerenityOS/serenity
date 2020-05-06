include_directories(${CMAKE_CURRENT_BINARY_DIR})

set(SOURCES
    FontEditor.cpp
    GlyphEditorWidget.cpp
    GlyphMapWidget.cpp
    main.cpp
)

serenity_bin(FontEditor)
target_link_libraries(FontEditor LibGUI LibGfx)
