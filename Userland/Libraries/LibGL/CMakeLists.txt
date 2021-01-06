set(SOURCES
    GLColor.cpp
    GLMat.cpp
    GLUtils.cpp
    GLVert.cpp
    SoftwareGLContext.cpp
)

serenity_lib(LibGL gl)
target_link_libraries(LibGL LibM LibCore)
