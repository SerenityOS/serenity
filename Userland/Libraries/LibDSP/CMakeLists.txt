set(SOURCES
    Clip.cpp
    Effects.cpp
    Synthesizers.cpp
    Keyboard.cpp
    Track.cpp
)

serenity_lib(LibDSP dsp)
target_link_libraries(LibDSP PRIVATE LibCore)
