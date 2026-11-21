set(SOURCES
    CommandBufferBuilder.cpp
    Device.cpp
    Image.cpp
    Shader.cpp

    ../LibGPU/Image.cpp
)

serenity_lib(LibVirtGPU virtgpu)
target_link_libraries(LibVirtGPU PRIVATE LibCore)
