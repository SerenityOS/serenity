set(SOURCES
    FileSystem.cpp
    TempFile.cpp
)

serenity_lib(LibFileSystem filesystem)
target_link_libraries(LibFileSystem PRIVATE LibCoreMinimal)

# ===== LibFileSystem for BuggieBox =====
add_library(LibFileSystemStatic STATIC ${SOURCES})
target_link_libraries(LibFileSystemStatic PRIVATE LibCoreStatic)
