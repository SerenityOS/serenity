set(TEST_SOURCES
    TestFeedFilter.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibDSP)
endforeach()
