set(SOURCES
    Origin.cpp
    Parser.cpp
    URL.cpp
)

serenity_lib(LibURL url)
target_link_libraries(LibURL PRIVATE LibUnicode LibTextCodec)

# ===== LibURL for BuggieBox =====
add_library(BuggieBox_LibURL STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibURL
    PUBLIC StaticPIE_CompileOptions
    PRIVATE BuggieBox_LibUnicode
)
