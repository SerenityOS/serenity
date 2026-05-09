set(TEST_SOURCES
    TestSSHCipher.cpp
    TestSSHEncodeDecode.cpp
    TestSSHIdentificationString.cpp
    TestSSHPeer.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibSSH LIBS LibSSH)
endforeach()
