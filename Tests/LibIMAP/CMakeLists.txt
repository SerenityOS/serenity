set(TEST_SOURCES
    TestQuotedPrintable.cpp
    TestMessageHeaderEncoding.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibIMAP LIBS LibIMAP)
endforeach()
