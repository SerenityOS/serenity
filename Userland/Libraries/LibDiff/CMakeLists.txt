
set(SOURCES
    Hunks.cpp
    Format.cpp
)

serenity_lib(LibDiff diff)
target_link_libraries(LibDiff LibC)
