set(SOURCES
    Clipper.cpp
    DepthBuffer.cpp
    Device.cpp
    Image.cpp
    Sampler.cpp
)

serenity_lib(LibSoftGPU softgpu)
target_link_libraries(LibSoftGPU LibM LibCore LibGfx)
