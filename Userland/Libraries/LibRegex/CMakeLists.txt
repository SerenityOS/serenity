set(SOURCES
    C/Regex.cpp
    RegexByteCode.cpp
    RegexLexer.cpp
    RegexMatcher.cpp
    RegexOptimizer.cpp
    RegexParser.cpp
)

serenity_lib(LibRegex regex)
target_link_libraries(LibRegex LibC LibCore LibUnicode)
