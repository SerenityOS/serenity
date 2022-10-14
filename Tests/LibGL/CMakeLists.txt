set(TEST_SOURCES
    TestAPI.cpp
    TestRender.cpp
    TestShaders.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGL LIBS LibCore LibGfx LibGL)
endforeach()

install(DIRECTORY reference-images DESTINATION usr/Tests/LibGL)
