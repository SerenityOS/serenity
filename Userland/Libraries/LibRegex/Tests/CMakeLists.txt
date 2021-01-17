file(GLOB TEST_SOURCES CONFIGURE_DEPENDS "*.cpp")
file(GLOB REGEX_SOURCES CONFIGURE_DEPENDS "../*.cpp" "../C/*.cpp")

foreach(source ${TEST_SOURCES})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name} ${source} ${REGEX_SOURCES})
    target_link_libraries(${name} LibCore)
    install(TARGETS ${name} RUNTIME DESTINATION usr/Tests/LibRegex)
endforeach()
