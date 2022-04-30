set(SOURCES
    GLAPI.cpp
    GLContext.cpp
    Tex/NameAllocator.cpp
    Tex/Texture2D.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx LibGPU)
