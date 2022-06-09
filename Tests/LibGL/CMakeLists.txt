set(TEST_SOURCES
    TestAPI.cpp
    TestRender.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGL LIBS LibCore LibGL)
endforeach()

install(DIRECTORY reference-images DESTINATION usr/Tests/LibGL)
