serenity_component(
    Piano
    RECOMMENDED
    TARGETS Piano
    DEPENDS AudioServer
)

set(SOURCES
    Track.cpp
    TrackManager.cpp
    KeysWidget.cpp
    KnobsWidget.cpp
    main.cpp
    MainWidget.cpp
    RollWidget.cpp
    SamplerWidget.cpp
    WaveWidget.cpp
    DSP/DevProcessors.cpp
    DSP/Clip.cpp
    DSP/Track.cpp
)

serenity_app(Piano ICON app-piano)
target_link_libraries(Piano LibAudio LibGUI)
