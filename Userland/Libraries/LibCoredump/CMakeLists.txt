set(SOURCES
    Backtrace.cpp
    Inspector.cpp
    Reader.cpp
)

serenity_lib(LibCoredump coredump)
target_link_libraries(LibCoredump LibC LibCompress LibCore LibDebug)
