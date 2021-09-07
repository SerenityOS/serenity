include(commonmark_spec)

set(TEST_SOURCES
    TestCommonmark.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibMarkdown LIBS LibMarkdown)
endforeach()
