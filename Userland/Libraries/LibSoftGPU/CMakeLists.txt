set(SOURCES
    Clipper.cpp
    Device.cpp
    Image.cpp
    Sampler.cpp
)

add_compile_options(-Wno-psabi)
serenity_lib(LibSoftGPU softgpu)
target_link_libraries(LibSoftGPU LibM LibCore LibGfx)
