set(TEST_SOURCES
    TestRender.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGL LIBS LibGL)
endforeach()
