set(SOURCES
    Main.cpp
)

serenity_lib(LibMain main TYPE STATIC)

# ===== BuggieBox_LibMain for BuggieBox =====
add_library(BuggieBox_LibMain STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibMain
    PUBLIC StaticPIE_CompileOptions
)
