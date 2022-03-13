set(SOURCES
    GLBlend.cpp
    GLColor.cpp
    GLContext.cpp
    GLDraw.cpp
    GLFog.cpp
    GLLights.cpp
    GLLists.cpp
    GLMap.cpp
    GLMatrix.cpp
    GLStencil.cpp
    GLTexture.cpp
    GLUtils.cpp
    GLVert.cpp
    GLVertexArrays.cpp
    Tex/NameAllocator.cpp
    Tex/Texture2D.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore LibGfx LibSoftGPU)
