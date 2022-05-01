set(SOURCES
    ContextParameter.cpp
    GLAPI.cpp
    GLContext.cpp
    Matrix.cpp
    Lighting.cpp
    Lists.cpp
    Stencil.cpp
    Tex/NameAllocator.cpp
    Tex/Texture2D.cpp
    Textures.cpp
    Vertex.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx LibGPU)
