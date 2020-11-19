file(GLOB TEST_SOURCES CONFIGURE_DEPENDS "*.cpp")
file(GLOB REGEX_SOURCES CONFIGURE_DEPENDS "../*.cpp" "../C/*.cpp")

foreach(source ${TEST_SOURCES})
    get_filename_component(name ${source} NAME_WE)
    add_executable(${name} ${source} ${REGEX_SOURCES})
    target_link_libraries(${name} LagomCore)
    add_test(
        NAME ${name}
        COMMAND ${name}
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )

    set_tests_properties(
        ${name}
        PROPERTIES
            FAIL_REGULAR_EXPRESSION
            "FAIL"
    )
endforeach()
