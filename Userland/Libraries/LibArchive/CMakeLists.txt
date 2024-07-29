set(SOURCES
        Tar.cpp
        TarStream.cpp
        Zip.cpp
        )

serenity_lib(LibArchive archive)
target_link_libraries(LibArchive PRIVATE LibCompress LibCore LibCrypto)

# ===== LibArchive for BuggieBox =====
add_library(BuggieBox_LibArchive STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibArchive
    PUBLIC StaticPIE_CompileOptions
    PRIVATE BuggieBox_LibCompress BuggieBox_LibCore BuggieBox_LibCrypto
)
