set(SOURCES
    Brotli.cpp
    BrotliDictionary.cpp
    Deflate.cpp
    Lzma.cpp
    Lzma2.cpp
    PackBitsDecoder.cpp
    Xz.cpp
    Zlib.cpp
    Gzip.cpp
)

serenity_lib(LibCompress compress)
target_link_libraries(LibCompress PRIVATE LibCore LibCrypto)

# ===== LibCompress for BuggieBox =====
serenity_static_lib(LibCompressStatic)
target_link_libraries(LibCompressStatic PRIVATE LibCoreStatic LibCryptoStatic)
