
set(SOURCES
    example.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")

add_library(ExampleSO SHARED ${SOURCES})
#target_link_libraries(ExampleSO LibCShared)
target_link_libraries(ExampleSO)
install(TARGETS ExampleSO DESTINATION usr/lib)
