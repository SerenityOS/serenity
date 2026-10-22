set(TEST_SOURCES
    TestAML.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibACPI LIBS LibACPI)
endforeach()

install(DIRECTORY test-inputs DESTINATION usr/Tests/LibACPI)
