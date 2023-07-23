set(TEST_SOURCES
    test-parser.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGLSL LIBS LibGLSL)
endforeach()
