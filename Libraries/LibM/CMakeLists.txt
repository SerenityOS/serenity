set(SOURCES
    math.cpp
)

serenity_libc(LibM m)
target_link_libraries(LibM LibC)
