set(TEST_SOURCES
    TestLibPthreadSpinLocks.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibPthread LIBS LibPthread)
endforeach()
