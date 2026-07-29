add_subdirectory(Wait)

set(TEST_SOURCES
    TestFIFO.cpp
    TestPathLength.cpp
    TestPthreadCond.cpp
)

foreach(libtest_source IN LISTS TEST_SOURCES)
    serenity_test("${libtest_source}" Kernel)
endforeach()
