set(TEST_SOURCES
    TestVP9Decode.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibVideo LIBS LibVideo)
endforeach()

install(FILES vp9_in_webm.webm DESTINATION usr/Tests/LibVideo)
install(FILES vp9_4k.webm DESTINATION usr/Tests/LibVideo)
