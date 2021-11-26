set(TEST_SOURCES
    test-run-ls.cpp
    ue-write-oob.cpp
)

# FIXME: These tests do not use LibTest
foreach(source IN LISTS TEST_SOURCES)
    get_filename_component(test_name "${source}" NAME_WE)
    add_executable("${test_name}" "${source}")
    target_link_libraries("${test_name}" LibCore)
    install(TARGETS "${test_name}" RUNTIME DESTINATION usr/Tests/UserEmulator)
endforeach()
