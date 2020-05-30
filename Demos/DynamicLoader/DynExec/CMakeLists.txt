set(SOURCES
    main.cpp
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -pie")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic -pie")
#set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} --dynamic-linker=/bin/Loader")

add_executable(DynExec ${SOURCES})
install(TARGETS DynExec RUNTIME DESTINATION bin)
set_property(TARGET DynExec PROPERTY POSITION_INDEPENDENT_CODE ON)
target_link_libraries(DynExec ExampleSO)
