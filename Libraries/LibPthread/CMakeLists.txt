set(SOURCES
    pthread.cpp
)

serenity_libc(LibPthread pthread)
target_link_libraries(LibPthread LibC)
target_include_directories(LibPthread PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
