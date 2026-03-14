set(SOURCES
        Tar.cpp
        TarStream.cpp
        Zip.cpp
        )

serenity_lib(LibArchive archive)
target_link_libraries(LibArchive PRIVATE LibCompress LibCore LibCrypto)

serenity_static_lib(LibArchiveStatic)
target_link_libraries(LibArchiveStatic PRIVATE LibCompressStatic LibCoreStatic LibCryptoStatic)
