set(TEST_SOURCES
    TestParseMatroska.cpp
    TestVP9Decode.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibVideo LIBS LibVideo)
endforeach()

install(FILES vp9_in_webm.webm DESTINATION usr/Tests/LibVideo)
install(FILES vp9_4k.webm DESTINATION usr/Tests/LibVideo)
install(FILES vp9_clamp_reference_mvs.webm DESTINATION usr/Tests/LibVideo)
install(FILES vp9_oob_blocks.webm DESTINATION usr/Tests/LibVideo)
install(FILES master_elements_containing_crc32.mkv DESTINATION usr/Tests/LibVideo)
