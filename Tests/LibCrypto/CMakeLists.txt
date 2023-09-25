set(TEST_SOURCES
    TestAES.cpp
    TestASN1.cpp
    TestBigInteger.cpp
    TestChecksum.cpp
    TestChaCha20.cpp
    TestChacha20Poly1305.cpp
    TestCurves.cpp
    TestEd25519.cpp
    TestHash.cpp
    TestHMAC.cpp
    TestPBKDF2.cpp
    TestPoly1305.cpp
    TestRSA.cpp
)

foreach(source IN LISTS TEST_SOURCES)
    serenity_test("${source}" LibCrypto LIBS LibCrypto)
endforeach()
