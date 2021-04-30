set(SOURCES
    BackgroundAction.cpp
    Thread.cpp
)

serenity_lib(LibThread thread)
target_link_libraries(LibThread ${LIBRARY_LIBC} LibCore ${LIBRARY_LIBPTHREAD})
