set(SOURCES
    main.cpp
    ../../Libraries/LibC/crt0_shared.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostartfiles -lgcc_s -pie -fpic ")

serenity_bin(DynamicObjectDemo)
target_link_libraries(DynamicObjectDemo SampleLib LibC LibCore LibGUI)

add_subdirectory(SampleLib)
