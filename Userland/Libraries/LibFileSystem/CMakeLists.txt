set(SOURCES
    FileSystem.cpp
    TempFile.cpp
)

serenity_lib(LibFileSystem filesystem)
target_link_libraries(LibFileSystem PRIVATE LibCoreMinimal)

# ===== LibFileSystem for BuggieBox =====
add_library(BuggieBox_LibFileSystem STATIC ${SOURCES})
target_link_libraries(BuggieBox_LibFileSystem
    PUBLIC StaticPIE_CompileOptions
    PRIVATE BuggieBox_LibCore
)
