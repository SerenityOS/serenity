file(GLOB CMD_SOURCES CONFIGURE_DEPENDS "*.cpp")

foreach(CMD_SRC ${CMD_SOURCES})
    get_filename_component(CMD_NAME ${CMD_SRC} NAME_WE)
    add_executable(${CMD_NAME}_Userland ${CMD_SRC})
    set_target_properties(${CMD_NAME}_Userland PROPERTIES OUTPUT_NAME ${CMD_NAME})
    target_link_libraries(${CMD_NAME}_Userland LibCore)
    install(TARGETS ${CMD_NAME}_Userland RUNTIME DESTINATION usr/Tests/AK)
endforeach()
