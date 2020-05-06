set(SOURCES
    math.cpp
)

serenity_libc(LibM m)
target_include_directories(LibM PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(LibM LibC)
