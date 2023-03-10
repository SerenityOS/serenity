set(TEST_SOURCES
    BenchmarkGfxPainter.cpp
    TestFontHandling.cpp
    TestICCProfile.cpp
    TestImageDecoder.cpp
    TestScalingFunctions.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGfx LIBS LibGfx)
endforeach()

install(DIRECTORY test-inputs DESTINATION usr/Tests/LibGfx)
