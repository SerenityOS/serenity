set(SOURCES
    Origin.cpp
    Parser.cpp
    URL.cpp
)

serenity_lib(LibURL url)
target_link_libraries(LibURL PRIVATE LibUnicode LibTextCodec)

serenity_static_lib(LibURLStatic)
target_link_libraries(LibURLStatic PRIVATE LibUnicodeStatic LibTextCodecStatic)
