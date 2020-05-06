set(SOURCES
    DynamicLib.cpp
)

add_library(DynamicLib SHARED ${SOURCES})
target_link_libraries(DynamicLib LibC)
install(TARGETS DynamicLib DESTINATION usr/lib)
