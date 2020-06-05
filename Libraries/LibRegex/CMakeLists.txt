set(SOURCES
    RegexByteCode.cpp
    RegexLexer.cpp
    RegexMatcher.cpp
    RegexParser.cpp
)

serenity_lib(LibRegex regex)
target_link_libraries(LibRegex LibC LibCore)
