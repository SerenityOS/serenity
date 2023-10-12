set(SOURCES
    Types.cpp
)

serenity_lib(LibRIFF riff)
target_link_libraries(LibRIFF PRIVATE LibCore)
