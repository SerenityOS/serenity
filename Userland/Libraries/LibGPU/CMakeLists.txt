set(SOURCES
    Driver.cpp
)

serenity_lib(LibGPU gpu)
target_link_libraries(LibGPU LibCore ${CMAKE_DL_LIBS})

add_dependencies(LibGPU LibSoftGPU)
