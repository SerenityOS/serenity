set(TEST_SOURCES
    TestBrotli.cpp
    TestDeflate.cpp
    TestGzip.cpp
    TestZlib.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibCompress LIBS LibCompress)
endforeach()

install(DIRECTORY brotli-test-files DESTINATION usr/Tests/LibCompress)
