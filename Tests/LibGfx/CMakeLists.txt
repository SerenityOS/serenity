set(TEST_SOURCES
    BenchmarkGfxPainter.cpp
    TestDeltaE.cpp
    TestFontHandling.cpp
    TestGfxBitmap.cpp
    TestICCProfile.cpp
    TestParseISOBMFF.cpp
    TestRect.cpp
    TestScalingFunctions.cpp
    TestWOFF.cpp
    TestWOFF2.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGfx LIBS LibGfx LibMedia)
endforeach()

install(DIRECTORY test-inputs DESTINATION usr/Tests/LibGfx)
