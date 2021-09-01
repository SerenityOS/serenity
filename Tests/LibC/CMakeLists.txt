set(TEST_SOURCES
    TestIo.cpp
    TestLibCExec.cpp
    TestLibCDirEnt.cpp
    TestLibCInodeWatcher.cpp
    TestLibCMkTemp.cpp
    TestLibCSetjmp.cpp
    TestLibCString.cpp
    TestLibCTime.cpp
    TestMemmem.cpp
    TestQsort.cpp
    TestRealpath.cpp
    TestScanf.cpp
    TestSnprintf.cpp
    TestStackSmash.cpp
    TestStrlcpy.cpp
    TestStrtodAccuracy.cpp
)

set_source_files_properties(TestStrtodAccuracy.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin-strtod")

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibC)
endforeach()
