set(SOURCES
    Clip.cpp
    Track.cpp
    Effects.cpp
    Synthesizers.cpp
    Keyboard.cpp
    BandAdjustmentFilter.cpp
)

serenity_lib(LibDSP dsp)
target_link_libraries(LibDSP LibCore)
