set(TEST_SOURCES
    TestSSHIdentificationString.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibSSH LIBS LibSSH)
endforeach()
