set(TEST_SOURCES
    TestUnicodeCharacterTypes.cpp
    TestUnicodeLocale.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibUnicode LIBS LibUnicode)
endforeach()
