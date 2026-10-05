set(SOURCES
    Buffer.cpp
    Device.cpp
    Image.cpp
    Shader.cpp

    ../LibGPU/Image.cpp
)

serenity_lib(LibV3DGPU v3dgpu)
target_link_libraries(LibV3DGPU PRIVATE LibCore)
