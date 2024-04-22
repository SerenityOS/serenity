set(CMAKE_SKIP_RPATH FALSE)

macro(add_test_lib NAME FILE)
    add_library(${NAME} SHARED ${FILE})
    serenity_set_implicit_links(${NAME})
    install(TARGETS ${NAME} DESTINATION usr/Tests/LibELF)
endmacro()

macro(add_test_exe NAME FILE)
    add_executable(${NAME} ${FILE})
    serenity_set_implicit_links(${NAME})
    install(TARGETS ${NAME} DESTINATION usr/Tests/LibELF)
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
    TestOrder.cpp
    TestTLS.cpp
    TestWeakSymbolResolution.cpp
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

add_test_lib(TestWeakSymbolResolution1 TestWeakSymbolResolution1.cpp)
add_test_lib(TestWeakSymbolResolution2 TestWeakSymbolResolution2.cpp)
target_link_libraries(TestWeakSymbolResolution PRIVATE TestWeakSymbolResolution1 TestWeakSymbolResolution2)
set_target_properties(TestWeakSymbolResolution PROPERTIES INSTALL_RPATH "$ORIGIN")

add_test_lib(TestOrderLib1 TestOrderLib1.cpp)
add_test_lib(TestOrderLib2 TestOrderLib2.cpp)
target_link_libraries(TestOrderLib2 PRIVATE TestOrderLib1)
set_target_properties(TestOrderLib2 PROPERTIES INSTALL_RPATH "$ORIGIN")

# NOTE: This is so ugly because CMake sorts targets supplied to target_link_libraries.
#       .elf extension here avoids direct invocations by SerenityOS's test runner.
add_test_exe(TestOrderExe1.elf TestOrderExe.cpp)
target_link_libraries(TestOrderExe1.elf PRIVATE $<TARGET_FILE:TestOrderLib1> $<TARGET_FILE:TestOrderLib2>)
add_dependencies(TestOrderExe1.elf TestOrderLib1 TestOrderLib2)
set_target_properties(TestOrderExe1.elf PROPERTIES INSTALL_RPATH "$ORIGIN")

add_test_exe(TestOrderExe2.elf TestOrderExe.cpp)
target_link_libraries(TestOrderExe2.elf PRIVATE $<TARGET_FILE:TestOrderLib2> $<TARGET_FILE:TestOrderLib1>)
add_dependencies(TestOrderExe2.elf TestOrderLib1 TestOrderLib2)
set_target_properties(TestOrderExe2.elf PROPERTIES INSTALL_RPATH "$ORIGIN")
