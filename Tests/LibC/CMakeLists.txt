set(TEST_SOURCES
    TestAbort.cpp
    TestAssert.cpp
    TestCType.cpp
    TestEnvironment.cpp
    TestIo.cpp
    TestLibCExec.cpp
    TestLibCDirEnt.cpp
    TestLibCInodeWatcher.cpp
    TestLibCMkTemp.cpp
    TestLibCNetdb.cpp
    TestLibCSetjmp.cpp
    TestLibCString.cpp
    TestLibCTime.cpp
    TestMalloc.cpp
    TestMath.cpp
    TestMemalign.cpp
    TestMemmem.cpp
    TestMkDir.cpp
    TestPthreadCancel.cpp
    TestPthreadCleanup.cpp
    TestPThreadPriority.cpp
    TestPthreadSpinLocks.cpp
    TestPthreadRWLocks.cpp
    TestPwd.cpp
    TestQsort.cpp
    TestRaise.cpp
    TestRealpath.cpp
    TestScanf.cpp
    TestSearch.cpp
    TestSnprintf.cpp
    TestStackSmash.cpp
    TestStdio.cpp
    TestStrlcpy.cpp
    TestStrtodAccuracy.cpp
    TestWchar.cpp
    TestWctype.cpp
)

set_source_files_properties(TestMath.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin")
set_source_files_properties(TestStrtodAccuracy.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin-strtod")

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibC)
endforeach()
