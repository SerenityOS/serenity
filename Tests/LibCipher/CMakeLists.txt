set(TEST_SOURCES
    TestAES.cpp
    TestBigInteger.cpp
    TestChecksum.cpp
    TestHash.cpp
    TestHMAC.cpp
    TestRSA.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibCipher LIBS LibCipher)
endforeach()
