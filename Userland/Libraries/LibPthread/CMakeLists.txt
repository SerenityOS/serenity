set(SOURCES
    pthread.cpp
    pthread_once.cpp
)

serenity_libc(LibPthread pthread)
target_link_libraries(LibPthread LibC LibSystem)
target_include_directories(LibPthread PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
