set(SOURCES
    pthread.cpp
)

serenity_libc(LibPthread pthread)
target_link_libraries(LibPthread LibC)
target_include_directories(LibPthread PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

add_library(LibPthreadShared SHARED ${SOURCES})
target_include_directories(LibPthreadShared PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(LibPthreadShared LibCShared)
install(TARGETS LibPthreadShared DESTINATION usr/lib)
