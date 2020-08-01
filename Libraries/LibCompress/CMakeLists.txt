set(SOURCES
    Deflate.cpp
    Zlib.cpp
)

serenity_lib(LibCompress compression)
target_link_libraries(LibCompress LibC)
