include(${SerenityOS_SOURCE_DIR}/Meta/CMake/unicode_data.cmake)

set(SOURCES
    CharacterTypes.cpp
    CurrencyCode.cpp
    Emoji.cpp
    IDNA.cpp
    Normalize.cpp
    Punycode.cpp
    Segmentation.cpp
    String.cpp
    UnicodeUtils.cpp
    ${UNICODE_DATA_SOURCES}
)
set(GENERATED_SOURCES ${CURRENT_LIB_GENERATED})

serenity_lib(LibUnicode unicode)

target_compile_definitions(LibUnicode PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)
