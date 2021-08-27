serenity_component(
    Piano
    RECOMMENDED
    TARGETS Piano
    DEPENDS AudioServer
)

set(SOURCES
    AudioPlayerLoop.cpp
    Track.cpp
    TrackManager.cpp
    KeysWidget.cpp
    KnobsWidget.cpp
    main.cpp
    MainWidget.cpp
    PlayerWidget.cpp
    RollWidget.cpp
    SamplerWidget.cpp
    WaveWidget.cpp
    ProcessorParameterSlider.cpp
)

serenity_app(Piano ICON app-piano)
target_link_libraries(Piano LibAudio LibDSP LibGUI)
