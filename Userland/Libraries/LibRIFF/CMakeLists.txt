set(SOURCES
    Decoding.cpp
    Details.cpp
)

serenity_lib(LibRIFF riff)

# ===== LibRIFF for BuggieBox =====
add_library(BuggieBox_LibRIFF STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibRIFF
    PUBLIC StaticPIE_CompileOptions
)
