set(SOURCES
    Tex/NameAllocator.cpp
    Tex/Sampler2D.cpp
    Tex/Texture2D.cpp
    Tex/TextureUnit.cpp
    Clipper.cpp
    GLBlend.cpp
    GLColor.cpp
    GLContext.cpp
    GLDraw.cpp
    GLFog.cpp
    GLLights.cpp
    GLLists.cpp
    GLMat.cpp
    GLStencil.cpp
    GLTexture.cpp
    GLUtils.cpp
    GLVert.cpp
    GLVertexArrays.cpp
    SoftwareGLContext.cpp
    SoftwareRasterizer.cpp
    DepthBuffer.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx)
