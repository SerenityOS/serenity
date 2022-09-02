set(TEST_SOURCES
    TestUnicodeCharacterTypes.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibUnicode LIBS LibUnicode)

    get_filename_component(target "${source}" NAME_WLE)
endforeach()
