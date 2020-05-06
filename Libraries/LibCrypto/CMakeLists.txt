set(SOURCES
    BigInt/UnsignedBigInteger.cpp
    Cipher/AES.cpp
    Hash/MD5.cpp
    Hash/SHA1.cpp
    Hash/SHA2.cpp
    PK/RSA.cpp
)

serenity_lib(LibCrypto crypto)
target_link_libraries(LibCrypto LibC)
