set(SOURCES
    Brotli.cpp
    BrotliDictionary.cpp
    Deflate.cpp
    Zlib.cpp
    Gzip.cpp
)

serenity_lib(LibCompress compress)
target_link_libraries(LibCompress PRIVATE LibCore LibCrypto)

serenity_lib_static(LibCompressStatic compress)
target_link_libraries(LibCompressStatic PRIVATE LibCoreStatic LibCryptoStatic)
