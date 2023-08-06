set(TEST_SOURCES
    TestFLACSpec.cpp
    TestPlaybackStream.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibAudio LIBS LibAudio)
endforeach()

install(DIRECTORY ${FLAC_SPEC_TEST_PATH} DESTINATION usr/Tests/LibAudio/FLAC)
