set(TEST_SOURCES
    TestDateTimeFormat.cpp
    TestLocale.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibLocale LIBS LibUnicode)

    get_filename_component(target "${source}" NAME_WLE)
    link_with_locale_data("${target}")
endforeach()
