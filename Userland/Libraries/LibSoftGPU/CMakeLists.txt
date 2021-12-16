set(SOURCES
    Clipper.cpp
    DepthBuffer.cpp
    SoftwareRasterizer.cpp
)

serenity_lib(LibSoftGPU softgpu)
target_link_libraries(LibSoftGPU LibM LibCore LibGfx)
