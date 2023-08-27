set(SOURCES
    SemVer.cpp
)

serenity_lib(LibSemVer semver)
target_link_libraries(LibSemVer PRIVATE LibRegex LibUnicode)
