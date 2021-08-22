set(SOURCES
    Backtrace.cpp
    Reader.cpp
)

serenity_lib(LibCoredump Coredump)
target_link_libraries(LibCoredump LibC LibCompress LibCore LibDebug)
