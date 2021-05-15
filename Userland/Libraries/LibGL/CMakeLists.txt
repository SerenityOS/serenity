set(SOURCES
    Clipper.cpp
    GLBlend.cpp
    GLColor.cpp
    GLContext.cpp
    GLLists.cpp
    GLMat.cpp
    GLUtils.cpp
    GLVert.cpp
    SoftwareGLContext.cpp
    SoftwareRasterizer.cpp
    DepthBuffer.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx)
