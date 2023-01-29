include(commonmark_spec)

set(TEST_SOURCES
    TestCommonmark.cpp
    TestImageSizeExtension.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibMarkdown LIBS LibMarkdown)
endforeach()

if (BUILD_LAGOM)
    set_tests_properties(TestCommonmark PROPERTIES DISABLED YES)
endif()
