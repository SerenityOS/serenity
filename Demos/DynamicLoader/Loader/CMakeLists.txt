set(SOURCES
    main.cpp
)

#set(CMAKE_CXX_FLAGS "-fPIE ${CMAKE_CXX_FLAGS}")
#set_target_properties(Loader PROPERTIES OUTPUT_NAME Loader)

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib -fpic -pie")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
#set(LINKER_FLAGS "${LINKER_FLAGS} -")


add_executable(libc.so.1 ${SOURCES})
install(TARGETS libc.so.1 RUNTIME DESTINATION usr/lib/)
set_property(TARGET libc.so.1 PROPERTY POSITION_INDEPENDENT_CODE ON)
target_link_libraries(libc.so.1 LibC)

#serenity_bin(Loader)

#add_executable(Loader ${SOURCES})
#set_property(TARGET Loader PROPERTY POSITION_INDEPENDENT_CODE ON)
#target_link_libraries(Loader ExampleSO)
