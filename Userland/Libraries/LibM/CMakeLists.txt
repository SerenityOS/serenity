set(SOURCES
    math.cpp
    ../LibC/ssp.cpp
)

set_source_files_properties (../LibC/ssp.cpp PROPERTIES COMPILE_FLAGS
    "-fno-stack-protector")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib")
serenity_libc(LibM m)
#target_link_libraries(LibM)
#set_target_properties(LibM PROPERTIES OUTPUT_NAME m)
#target_link_directories(LibM PUBLIC ${CMAKE_CURRENT_BINARY_DIR})
