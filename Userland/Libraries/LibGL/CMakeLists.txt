include(libgl_generators)

set(SOURCES
    Blending.cpp
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

# Install symlinks at some common locations so ports can find LibGL without issue
if (SERENITYOS)
    install(CODE "
        file(CREATE_LINK LibGL/GL/ \${CMAKE_INSTALL_PREFIX}/usr/include/GL SYMBOLIC)
        file(CREATE_LINK libgl.so.serenity \${CMAKE_INSTALL_PREFIX}/usr/lib/libGL.so SYMBOLIC)
        file(CREATE_LINK libgl.so.serenity \${CMAKE_INSTALL_PREFIX}/usr/lib/libGL.so.1 SYMBOLIC)
    ")
endif()
