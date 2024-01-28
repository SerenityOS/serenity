set(TEST_SOURCES
    TestZonedDateTime.cpp
    TestDateTimeFormatting.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibDateTime LIBS LibDateTime)
endforeach()
