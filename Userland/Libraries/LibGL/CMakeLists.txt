set(SOURCES
    ClipPlane.cpp
    ContextParameter.cpp
    GLAPI.cpp
    GLContext.cpp
    Lighting.cpp
    List.cpp
    Matrix.cpp
    Stencil.cpp
    Tex/NameAllocator.cpp
    Tex/Texture2D.cpp
    Texture.cpp
    Vertex.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx LibGPU)
