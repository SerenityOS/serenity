set(SOURCES
    main.cpp
    Utils.cpp
    DynamicObject.cpp
    Heap.cpp
    List.cpp
    Syscalls.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -pie")

add_executable(Loader.so ${SOURCES})
install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)
