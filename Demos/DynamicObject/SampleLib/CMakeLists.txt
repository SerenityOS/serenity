
set(SOURCES
    lib.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic") 

add_library(SampleLib SHARED ${SOURCES})
target_link_libraries(SampleLib LibC)
#target_link_libraries(SampleLib)
install(TARGETS SampleLib DESTINATION usr/lib)
