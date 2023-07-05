set(CMAKE_SKIP_RPATH FALSE)

macro(add_test_lib NAME FILE)
    add_library(${NAME} SHARED ${FILE})
    serenity_set_implicit_links(${NAME})
    # Avoid execution by the test runner
    install(TARGETS ${NAME}
            DESTINATION usr/Tests/LibELF
            PERMISSIONS OWNER_READ GROUP_READ WORLD_READ OWNER_WRITE GROUP_WRITE)
endmacro()

macro(add_dlopen_lib NAME FUNCTION)
    add_test_lib(${NAME} Dynlib.cpp)
    target_compile_definitions(${NAME} PRIVATE -DFUNCTION=${FUNCTION})
    # LibLine is not special, just an "external" dependency
    target_link_libraries(${NAME} PRIVATE LibLine)
endmacro()

add_dlopen_lib(DynlibA dynliba_function)
add_dlopen_lib(DynlibB dynlibb_function)

add_dlopen_lib(DynlibC dynlibc_function)
set(CMAKE_INSTALL_RPATH $ORIGIN)
add_dlopen_lib(DynlibD dynlibd_function)
target_link_libraries(DynlibD PRIVATE DynlibC)
unset(CMAKE_INSTALL_RPATH)

set(TEST_SOURCES
    test-elf.cpp
    TestDlOpen.cpp
    TestTLS.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibELF)
endforeach()

add_test_lib(TLSDef TLSDef.cpp)
add_test_lib(TLSUse TLSUse.cpp)
target_compile_options(TLSUse PRIVATE -ftls-model=global-dynamic)
target_link_libraries(TLSUse PRIVATE LibCore LibTest LibThreading TLSDef)
set_target_properties(TLSUse PROPERTIES INSTALL_RPATH "$ORIGIN")
target_link_libraries(TestTLS PRIVATE TLSUse)
set_target_properties(TestTLS PROPERTIES INSTALL_RPATH "$ORIGIN")
