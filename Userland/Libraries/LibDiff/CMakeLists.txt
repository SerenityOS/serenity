
set(SOURCES
    Format.cpp
    Generator.cpp
    Hunks.cpp
)

serenity_lib(LibDiff diff)
target_link_libraries(LibDiff LibC)
