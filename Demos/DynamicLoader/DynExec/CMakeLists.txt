set(SOURCES
    main.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -pie")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -pie")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -pie")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic -pie")
#set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} --dynamic-linker=/bin/Loader")

add_executable(DynExec ${SOURCES})
install(TARGETS DynExec RUNTIME DESTINATION bin)
target_link_libraries(DynExec ExampleSO LibCShared)
#target_link_libraries(DynExec ExampleSO LibC)
