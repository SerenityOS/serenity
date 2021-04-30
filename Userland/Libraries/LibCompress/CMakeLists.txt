set(SOURCES
    Deflate.cpp
    Zlib.cpp
    Gzip.cpp
)

serenity_lib(LibCompress compress)
target_link_libraries(LibCompress ${LIBRARY_LIBC} LibCrypto)

add_subdirectory(Tests)
