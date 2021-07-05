set(SOURCES
    DevProcessors.cpp
    Clip.cpp
    Track.cpp
)

serenity_lib(LibDSP dsp)
target_link_libraries(LibDSP LibCore)
