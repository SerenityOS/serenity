set(TEST_SOURCES
    TestLibPthreadCleanup.cpp
    TestLibPthreadSpinLocks.cpp
    TestLibPthreadRWLocks.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibPthread LIBS LibPthread)
endforeach()
