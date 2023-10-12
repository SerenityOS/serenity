set(SOURCES
    Decoding.cpp
    Details.cpp
)

serenity_lib(LibRIFF riff)
target_link_libraries(LibRIFF PRIVATE LibCore)
