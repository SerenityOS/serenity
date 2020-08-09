set(SOURCES
    BigInt/UnsignedBigInteger.cpp
    BigInt/SignedBigInteger.cpp
    Checksum/Adler32.cpp
    Checksum/CRC32.cpp
    Cipher/AES.cpp
    Hash/MD5.cpp
    Hash/SHA1.cpp
    Hash/SHA2.cpp
    PK/RSA.cpp
)

serenity_lib(LibCrypto crypto)
target_link_libraries(LibCrypto LibC)
