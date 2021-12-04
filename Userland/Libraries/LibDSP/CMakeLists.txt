set(SOURCES
    Music.cpp
    Clip.cpp
    Track.cpp
    Effects.cpp
    Synthesizers.cpp
    FFT.cpp
    TrackManager.cpp
    Keyboard.cpp
)

serenity_lib(LibDSP dsp)
target_link_libraries(LibDSP LibCore)
