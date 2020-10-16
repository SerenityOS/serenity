set(SOURCES
    main.cpp
    ../../Libraries/LibC/crt0_shared.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_WITHOUT_STATIC} -nostartfiles -pie -fpic")

serenity_bin(DynamicObjectDemo)
target_link_libraries(DynamicObjectDemo SampleLib LibCShared)

add_subdirectory(SampleLib)
