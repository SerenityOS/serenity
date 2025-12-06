set(TEST_SOURCES
    TestMetrics.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibLine LIBS LibLine LibUnicode)
endforeach()
