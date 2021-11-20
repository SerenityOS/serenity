set(SOURCES
    Backtrace.cpp
    Inspector.cpp
    Reader.cpp
)

serenity_lib(LibCoredump Coredump)
target_link_libraries(LibCoredump LibC LibCompress LibCore LibDebug)
