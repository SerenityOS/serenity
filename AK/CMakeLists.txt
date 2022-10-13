set(AK_SOURCES
    Assertions.cpp
    Base64.cpp
    FloatingPointStringConversions.cpp
    FlyString.cpp
    Format.cpp
    FuzzyMatch.cpp
    GenericLexer.cpp
    Hex.cpp
    JsonParser.cpp
    JsonPath.cpp
    JsonValue.cpp
    kmalloc.cpp
    LexicalPath.cpp
    Random.cpp
    StackInfo.cpp
    String.cpp
    StringBuilder.cpp
    StringImpl.cpp
    StringUtils.cpp
    StringView.cpp
    Time.cpp
    URL.cpp
    URLParser.cpp
    Utf16View.cpp
    Utf8View.cpp
    UUID.cpp
)
# AK sources are included from many different places, such as the Kernel, LibC, and Loader
list(TRANSFORM AK_SOURCES PREPEND "${CMAKE_CURRENT_SOURCE_DIR}/")

set(AK_SOURCES ${AK_SOURCES} PARENT_SCOPE)

serenity_install_headers(AK)
serenity_install_sources(AK)
