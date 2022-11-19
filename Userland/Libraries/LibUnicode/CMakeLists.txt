include(${SerenityOS_SOURCE_DIR}/Meta/CMake/unicode_data.cmake)

set(SOURCES
    CharacterTypes.cpp
    CurrencyCode.cpp
    Emoji.cpp
    Normalize.cpp
    ${UNICODE_DATA_SOURCES}
)
set(GENERATED_SOURCES ${CURRENT_LIB_GENERATED})

serenity_lib(LibUnicode unicode)
target_link_libraries(LibUnicode PRIVATE LibCore)

serenity_lib_static(LibUnicodeStatic unicode)
target_link_libraries(LibUnicodeStatic PRIVATE LibCoreStatic)
target_compile_definitions(LibUnicode PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)
target_compile_definitions(LibUnicodeStatic PRIVATE ENABLE_UNICODE_DATA=$<BOOL:${ENABLE_UNICODE_DATABASE_DOWNLOAD}>)
