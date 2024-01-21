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

generate_libgl_implementation()

set(GENERATED_SOURCES
    GLAPI.cpp)

serenity_lib(LibGL gl)
target_link_libraries(LibGL PRIVATE LibGfx LibGLSL LibGPU)
