set(SOURCES
    pthread.cpp
)

serenity_libc(LibPthread pthread)
target_link_libraries(LibPthread LibC)
