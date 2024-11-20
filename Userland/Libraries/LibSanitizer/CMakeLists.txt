set(SOURCES
    UBSanitizer.cpp
    ../LibC/ssp.cpp
)

serenity_libc(LibUBSanitizer ubsan)
target_link_options(LibUBSanitizer PRIVATE -nostdlib)

add_library(DynamicLoader_LibUBSanitizer STATIC UBSanitizer.cpp)
target_link_libraries(DynamicLoader_LibUBSanitizer PUBLIC DynamicLoader_CompileOptions)
