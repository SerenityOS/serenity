set(TEST_SOURCES
    TestSed.cpp
    TestPatch.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" Utilities)
endforeach()
