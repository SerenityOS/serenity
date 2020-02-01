set(SOURCES
    main.cpp
    FontEditor.cpp
    GlyphEditorWidget.cpp
    GlyphMapWidget.cpp
)

add_custom_command(
    OUTPUT  UI_FontEditorBottom.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/../../DevTools/FormCompiler/FormCompiler ${CMAKE_CURRENT_SOURCE_DIR}/FontEditorBottom.frm > ${CMAKE_CURRENT_BINARY_DIR}/UI_FontEditorBottom.h
    DEPENDS FormCompiler FontEditorBottom.frm
)

set(SOURCES ${SOURCES} UI_FontEditorBottom.h)

serenity_app(FontEditor)
target_link_libraries(FontEditor LibGUI)
target_include_directories(FontEditor PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
