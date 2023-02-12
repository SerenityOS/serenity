include(libgl_generators)

set(SOURCES
    Buffer/Buffer.cpp
    Buffer.cpp
    ClipPlane.cpp
    ContextParameter.cpp
    GLContext.cpp
    Image.cpp
    Lighting.cpp
    List.cpp
    Matrix.cpp
    NameAllocator.cpp
    Shader.cpp
    Shaders/Program.cpp
    Shaders/Shader.cpp
    Stencil.cpp
    Tex/Texture2D.cpp
    Texture.cpp
    Vertex.cpp
)

# FIXME: Figure out what is going wrong here:
#        In the construction of the command list, we somewhere try to move a
#        const-ref which does not actually move, so us beign strict about
#        move-semantics breaks here, and the build-log is not very helpful to
#        look at.
add_compile_definitions(NO_STRICT_MOVE)

generate_libgl_implementation()

set(GENERATED_SOURCES
    GLAPI.cpp)

serenity_lib(LibGL gl)
target_link_libraries(LibGL PRIVATE LibCore LibGfx LibGLSL LibGPU)
