set(SOURCES
    Tex/NameAllocator.cpp
    Tex/Texture2D.cpp
    Tex/TextureUnit.cpp
    Clipper.cpp
    GLBlend.cpp
    GLColor.cpp
    GLContext.cpp
    GLLights.cpp
    GLLists.cpp
    GLMat.cpp
    GLTexture.cpp
    GLUtils.cpp
    GLVert.cpp
    SoftwareGLContext.cpp
    SoftwareRasterizer.cpp
    DepthBuffer.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx)
