include(${SerenityOS_SOURCE_DIR}/Meta/CMake/public_suffix.cmake)

set(SOURCES
    URL.cpp
    ${PUBLIC_SUFFIX_SOURCES}
)
set(GENERATED_SOURCES ${CURRENT_LIB_GENERATED})

serenity_lib(LibPublicSuffix publicsuffix)
target_compile_definitions(LibPublicSuffix PRIVATE ENABLE_PUBLIC_SUFFIX_DOWNLOAD=$<BOOL:${ENABLE_PUBLIC_SUFFIX_DOWNLOAD}>)
