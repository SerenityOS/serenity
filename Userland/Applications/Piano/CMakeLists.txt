serenity_component(
    Piano
    RECOMMENDED
    TARGETS Piano
    DEPENDS AudioServer
)

set(SOURCES
    AudioPlayerLoop.cpp
    KeysWidget.cpp
    KnobsWidget.cpp
    main.cpp
    MainWidget.cpp
    PlayerWidget.cpp
    RollWidget.cpp
    SamplerWidget.cpp
    TrackManager.cpp
    WaveWidget.cpp
    ProcessorParameterWidget/ParameterWidget.cpp
    ProcessorParameterWidget/Slider.cpp
)

serenity_app(Piano ICON app-piano)
target_link_libraries(Piano PRIVATE LibAudio LibCore LibDSP LibGfx LibGUI LibIPC LibMain LibThreading)
