set(SOURCES
    Clipper.cpp
    GLColor.cpp
    GLMat.cpp
    GLContext.cpp
    GLUtils.cpp
    GLVert.cpp
    SoftwareGLContext.cpp
    SoftwareRasterizer.cpp
    DepthBuffer.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx)
