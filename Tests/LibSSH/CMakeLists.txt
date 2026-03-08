set(TEST_SOURCES
    TestSSHIdentificationString.cpp
    TestSSHEncodeDecode.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibSSH LIBS LibSSH)
endforeach()
