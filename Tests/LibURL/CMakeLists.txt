set(URL_TEST_SOURCES
    TestURL.cpp
)

foreach(source IN LISTS URL_TEST_SOURCES)
    serenity_test("${source}" LibURL LIBS LibURL)
endforeach()
