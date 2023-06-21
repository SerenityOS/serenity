set(SOURCES
    Clipper.cpp
    Device.cpp
    Image.cpp
    PixelConverter.cpp
    ShaderCompiler.cpp
    ShaderProcessor.cpp
    Sampler.cpp
    Shader.cpp
)

add_compile_options(-Wno-psabi)
serenity_lib(LibSoftGPU softgpu)
target_link_libraries(LibSoftGPU PRIVATE LibCore LibGfx)
target_sources(LibSoftGPU PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/../LibGPU/Image.cpp")
