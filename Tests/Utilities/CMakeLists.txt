set(TEST_SOURCES
    TestPatch.cpp
    TestSed.cpp
    TestTest.cpp
    TestUniq.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" Utilities)
endforeach()
