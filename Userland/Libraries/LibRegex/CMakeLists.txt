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
target_link_libraries(LibRegex PRIVATE LibCore LibUnicode)

serenity_lib_static(LibRegexStatic regex)
target_link_libraries(LibRegexStatic PRIVATE LibCoreStatic LibUnicodeStatic)
