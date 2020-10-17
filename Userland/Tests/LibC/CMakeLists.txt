file(GLOB CMD_SOURCES  CONFIGURE_DEPENDS "*.cpp")

foreach(CMD_SRC ${CMD_SOURCES})
    get_filename_component(CMD_NAME ${CMD_SRC} NAME_WE)
    add_executable(${CMD_NAME} ${CMD_SRC})
    target_sources("${CMD_NAME}" PRIVATE ${CMAKE_SOURCE_DIR}/Libraries/LibC/crt0_shared.cpp)
    target_link_libraries(${CMD_NAME} LibCore)
    install(TARGETS ${CMD_NAME} RUNTIME DESTINATION usr/Tests/LibC)
endforeach()

#target_link_libraries(foobar LibPthread)
