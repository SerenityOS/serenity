set(SOURCES
    Backtrace.cpp
    Reader.cpp
)

serenity_lib(LibCoreDump coredump)
target_link_libraries(LibCoreDump LibC LibCore LibDebug)
