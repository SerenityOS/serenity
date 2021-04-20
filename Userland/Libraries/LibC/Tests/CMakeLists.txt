file(GLOB TEST_SOURCES CONFIGURE_DEPENDS "*.cpp")

foreach(source ${TEST_SOURCES})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name} ${source})
    target_link_libraries(${name} LibC LibCore)
    install(TARGETS ${name} RUNTIME DESTINATION usr/Tests/LibC)
endforeach()
