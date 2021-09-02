set(TEST_SOURCES
    BenchmarkGfxPainter.cpp
    TestFontHandling.cpp
    TestImageDecoder.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGfx LIBS LibGUI)
endforeach()
