set(TEST_SOURCES
    TestFromStringView.cpp
    TestSemVer.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibSemVer LIBS LibSemVer)
endforeach()
