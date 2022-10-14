set(TEST_SOURCES
    TestEDID.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibEDID LIBS LibEDID)
endforeach()
