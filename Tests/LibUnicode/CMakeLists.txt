set(TEST_SOURCES
    TestUnicodeCharacterTypes.cpp
    TestUnicodeDateTimeFormat.cpp
    TestUnicodeLocale.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibUnicode LIBS LibUnicode)

    get_filename_component(target "${source}" NAME_WLE)
    link_with_unicode_data("${target}")
endforeach()
