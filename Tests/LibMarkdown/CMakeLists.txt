include(${SERENITY_PROJECT_ROOT}/Meta/CMake/commonmark_spec.cmake)

set(TEST_SOURCES
    TestCommonmark.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibMarkdown LIBS LibMarkdown)
endforeach()
