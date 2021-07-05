set(SOURCES
    forward.cpp
    pthread.cpp
    pthread_cond.cpp
    pthread_once.cpp
    semaphore.cpp
)

serenity_libc(LibPthread pthread)
target_link_libraries(LibPthread LibC LibSystem)
target_include_directories(LibPthread PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
