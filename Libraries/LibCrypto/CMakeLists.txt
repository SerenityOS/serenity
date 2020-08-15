set(SOURCES
    BigInt/SignedBigInteger.cpp
    BigInt/UnsignedBigInteger.cpp
    Checksum/Adler32.cpp
    Checksum/CRC32.cpp
    Cipher/AES.cpp
    Hash/MD5.cpp
    Hash/SHA1.cpp
    Hash/SHA2.cpp
    NumberTheory/ModularFunctions.cpp
    PK/RSA.cpp
)

serenity_lib(LibCrypto crypto)
target_link_libraries(LibCrypto LibC)
