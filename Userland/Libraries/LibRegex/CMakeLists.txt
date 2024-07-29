set(SOURCES
    RegexByteCode.cpp
    RegexLexer.cpp
    RegexMatcher.cpp
    RegexOptimizer.cpp
    RegexParser.cpp
)

if(SERENITYOS)
    list(APPEND SOURCES C/Regex.cpp)
endif()

serenity_lib(LibRegex regex)
target_link_libraries(LibRegex PRIVATE LibUnicode)

# ===== LibRegex for BuggieBox =====
add_library(BuggieBox_LibRegex STATIC ${SOURCES} ${GENERATED_SOURCES})
target_link_libraries(BuggieBox_LibRegex
    PUBLIC StaticPIE_CompileOptions
)
