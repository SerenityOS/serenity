set(SOURCES
    CommandBufferBuilder.cpp
    Device.cpp
    Image.cpp
    Shader.cpp
)

serenity_lib(LibVirtGPU virtgpu)
target_link_libraries(LibVirtGPU PRIVATE LibCore)
target_sources(LibVirtGPU PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/../LibGPU/Image.cpp")
