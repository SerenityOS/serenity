set(TEST_SOURCES
    TestAPI.cpp
    TestRender.cpp
    TestShaders.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibGL LIBS LibCore LibGfx LibGL LibGPU LibSoftGPU)
endforeach()

install(DIRECTORY reference-images DESTINATION usr/Tests/LibGL)
