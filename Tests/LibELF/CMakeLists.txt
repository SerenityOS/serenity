set(CMAKE_SKIP_RPATH FALSE)
if (NOT BUILD_LAGOM)
    set(CMAKE_INSTALL_RPATH "$ORIGIN:$ORIGIN/../lib")
    set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
else()
    set(CMAKE_BUILD_RPATH "$ORIGIN:$ORIGIN/../lib")
    set(CMAKE_INSTALL_RPATH "$ORIGIN:$ORIGIN/../lib")
endif()

macro(add_test_lib NAME FILE)
    add_library(${NAME} SHARED ${FILE})
    if (NOT BUILD_LAGOM)
        serenity_set_implicit_links(${NAME})
    else()
        target_link_libraries(${NAME} PRIVATE AK LibCore)
    endif()
    install(TARGETS ${NAME} DESTINATION usr/Tests/LibELF)
endmacro()

macro(add_test_exe NAME FILE)
    add_executable(${NAME} ${FILE})
    if (NOT BUILD_LAGOM)
        serenity_set_implicit_links(${NAME})
    else()
        target_link_libraries(${NAME} PRIVATE AK LibCore)
    endif()
    install(TARGETS ${NAME} DESTINATION usr/Tests/LibELF)
endmacro()

macro(add_dlopen_lib NAME FUNCTION)
    add_test_lib(${NAME} Dynlib.cpp)
    target_compile_definitions(${NAME} PRIVATE -DFUNCTION=${FUNCTION})
    # LibLine is not special, just an "external" dependency
    target_link_libraries(${NAME} PRIVATE LibLine)
endmacro()

macro(target_link_manual TARGET LIB)
    target_link_libraries(${TARGET} PRIVATE -Wl,--push-state,--no-as-needed $<TARGET_FILE:${LIB}> -Wl,--pop-state)
    add_dependencies(${TARGET} ${LIB})
endmacro()

set(TEST_SOURCES
    TestDlOpen.cpp
    TestOrder.cpp
    TestTLS.cpp
    TestWeakSymbolResolution.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    if (NOT BUILD_LAGOM)
        serenity_test("${source}" LibELF)
    else()
        get_filename_component(test_name ${source} NAME_WE)
        lagom_test("${source}" WORKING_DIRECTORY "$<TARGET_FILE_DIR:${test_name}>")
        # FIXME: Figure out why on Ubuntu 22.04 dlopen ignores RUNPATH in executable.
        set_tests_properties(${test_name} PROPERTIES ENVIRONMENT LD_LIBRARY_PATH=${CMAKE_LIBRARY_OUTPUT_DIRECTORY})
    endif()
endforeach()

# test-elf.cpp
if (NOT BUILD_LAGOM)
    serenity_test(test-elf.cpp LibELF)
endif()

# TestDlOpen.cpp
add_dlopen_lib(DynlibA dynliba_function)
add_dlopen_lib(DynlibB dynlibb_function)

add_dlopen_lib(DynlibC dynlibc_function)
add_dlopen_lib(DynlibD dynlibd_function)
target_link_manual(DynlibD DynlibC)

# TestTLS.cpp
add_test_lib(TLSDef TLSDef.cpp)
add_test_lib(TLSUse TLSUse.cpp)
target_compile_options(TLSUse PRIVATE -ftls-model=global-dynamic)
target_link_libraries(TLSUse PRIVATE LibCore LibTest LibThreading TLSDef)
target_link_libraries(TestTLS PRIVATE TLSUse)

# TestWeakSymbolResolution.cpp
add_test_lib(TestWeakSymbolResolution1 TestWeakSymbolResolution1.cpp)
add_test_lib(TestWeakSymbolResolution2 TestWeakSymbolResolution2.cpp)
target_link_manual(TestWeakSymbolResolution TestWeakSymbolResolution1)
target_link_manual(TestWeakSymbolResolution TestWeakSymbolResolution2)

# TestOrder.cpp
add_test_lib(TestOrderLib1 TestOrderLib1.cpp)
add_test_lib(TestOrderLib2 TestOrderLib2.cpp)
target_link_manual(TestOrderLib2 TestOrderLib1)

add_test_exe(TestOrderExe1.elf TestOrderExe.cpp)
target_link_manual(TestOrderExe1.elf TestOrderLib1)
target_link_manual(TestOrderExe1.elf TestOrderLib2)

add_test_exe(TestOrderExe2.elf TestOrderExe.cpp)
target_link_manual(TestOrderExe2.elf TestOrderLib2)
target_link_manual(TestOrderExe2.elf TestOrderLib1)

add_dependencies(TestOrder
    TestOrderLib1
    TestOrderLib2
    TestOrderExe1.elf
    TestOrderExe2.elf
)
