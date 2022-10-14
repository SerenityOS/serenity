set(SOURCES
    Driver.cpp
    Image.cpp
)

serenity_lib(LibGPU gpu)
target_link_libraries(LibGPU PRIVATE LibCore ${CMAKE_DL_LIBS})

add_dependencies(LibGPU LibSoftGPU)

if (SERENITYOS)
    add_dependencies(LibGPU LibVirtGPU)
endif()
