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
add_library(BuggieBox_LibCompress STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibCompress
    PUBLIC StaticPIE_CompileOptions
    PRIVATE BuggieBox_LibCore BuggieBox_LibCrypto
)
