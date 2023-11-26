include(accelerated_graphics)

if (HAS_ACCELERATED_GRAPHICS)
    set(SOURCES
        GL.cpp
        GlyphAtlas.cpp
        Context.cpp
        Painter.cpp
        Program.cpp
    )

    serenity_lib(LibAccelGfx accelgfx)
    target_link_libraries(LibAccelGfx PRIVATE LibGfx ${ACCEL_GFX_LIBS})
endif()
