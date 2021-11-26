macro(add_dlopen_lib NAME FUNCTION)
    add_library(${NAME} SHARED Dynlib.cpp)
    target_compile_definitions(${NAME} PRIVATE -DFUNCTION=${FUNCTION})
    # LibLine is not special, just an "external" dependency
    target_link_libraries(${NAME} LibLine)
	 # Avoid execution by the test runner
    install(TARGETS ${NAME}
            DESTINATION usr/Tests/LibELF
            PERMISSIONS OWNER_READ GROUP_READ WORLD_READ)
endmacro()
add_dlopen_lib(DynlibA dynliba_function)
add_dlopen_lib(DynlibB dynlibb_function)

set(TEST_SOURCES
    test-elf.cpp
    TestDlOpen.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibELF LIBS LibDl)
endforeach()
