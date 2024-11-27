set(TEST_SOURCES
    BenchmarkSegmenter.cpp
    TestDateTimeFormat.cpp
    TestLocale.cpp
    TestSegmenter.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibLocale LIBS LibLocale)

    get_filename_component(target "${source}" NAME_WLE)
endforeach()
