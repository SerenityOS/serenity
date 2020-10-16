
set(SOURCES
    lib.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_WIHTOUT_STATIC} -nostdlib -fpic") 

add_library(SampleLib SHARED ${SOURCES})
target_link_libraries(SampleLib LibCShared)
#target_link_libraries(SampleLib)
install(TARGETS SampleLib DESTINATION usr/lib)
