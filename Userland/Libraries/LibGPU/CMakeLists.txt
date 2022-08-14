set(SOURCES
    Driver.cpp
)

serenity_lib(LibGPU gpu)
target_link_libraries(LibGPU LibCore)

add_dependencies(LibGPU LibSoftGPU)
