set(TEST_SOURCES
    TestAbort.cpp
    TestAssert.cpp
    TestCType.cpp
    TestEnvironment.cpp
    TestFenv.cpp
    TestIo.cpp
    TestLibCExec.cpp
    TestLibCGrp.cpp
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
# Don't assume default rounding behavior is used for testing rounding behavior modifications.
# Clang doesn't seem support this on RISC-V, though (see comment on opaque_add() ).
if (NOT (CMAKE_CXX_COMPILER_ID MATCHES "Clang" AND "${SERENITY_ARCH}" STREQUAL "riscv64"))
    set_source_files_properties(TestFenv.cpp PROPERTIES COMPILE_FLAGS "-frounding-math")
endif()

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibC)
endforeach()
