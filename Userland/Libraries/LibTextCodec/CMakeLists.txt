include(libtextcodec_generators)

set(SOURCES
    Decoder.cpp
)

generate_encoding_indexes()

set(GENERATED_SOURCES
    LookupTables.cpp
)

serenity_lib(LibTextCodec textcodec)

# ===== LibTextCodec for BuggieBox =====
add_library(BuggieBox_LibTextCodec STATIC ${SOURCES} ${GENERATED_SOURCES})
target_link_libraries(BuggieBox_LibTextCodec
    PUBLIC StaticPIE_CompileOptions
)
