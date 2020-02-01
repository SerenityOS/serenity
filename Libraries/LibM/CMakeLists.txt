set(SOURCES
    math.cpp
)

serenity_libc(LibM m)
target_include_directories(LibM PUBLIC ${SERENITY_ROOT}/Libraries/LibM)
target_link_libraries(LibM LibC)
