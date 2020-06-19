set(SOURCES
    main.cpp
)

set(CMAKE_CXX_FLAGS " -static ${CMAKE_CXX_FLAGS}")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS_STATIC}")
#set(CMAKE_CXX_FLAGS "-fPIE ${CMAKE_CXX_FLAGS}")
#set_target_properties(Loader PROPERTIES OUTPUT_NAME Loader)

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic -pie")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
#set(LINKER_FLAGS "${LINKER_FLAGS} -")


add_executable(Loader.so ${SOURCES})
install(TARGETS Loader.so RUNTIME DESTINATION usr/lib/)
#set_property(TARGET Loader.so PROPERTY POSITION_INDEPENDENT_CODE ON)
target_link_libraries(Loader.so LibC LibM)

