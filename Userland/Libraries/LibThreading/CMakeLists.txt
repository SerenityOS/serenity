set(SOURCES
    BackgroundAction.cpp
    Thread.cpp
)

serenity_lib(LibThreading threading)
target_link_libraries(LibThreading PRIVATE LibCore)

# ===== LibThreading for BuggieBox =====
add_library(BuggieBox_LibThreading STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibThreading
    PUBLIC StaticPIE_CompileOptions
    PRIVATE BuggieBox_LibCore
)
