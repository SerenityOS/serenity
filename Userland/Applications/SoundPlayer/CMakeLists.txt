serenity_component(
    SoundPlayer
    RECOMMENDED
    TARGETS SoundPlayer
    DEPENDS AudioServer
)

set(SOURCES
    main.cpp
    AmpButton.cpp
    AmpPlayerView.cpp
    AmpSlider.cpp
    AmpTimeDisplay.cpp
    AmpToggleButton.cpp
    AmpWidget.cpp
    AudioAlgorithms.cpp
    BarsVisualizationWidget.cpp
    M3UParser.cpp
    NoVisualizationWidget.cpp
    PlaybackManager.cpp
    Player.cpp
    Playlist.cpp
    PlaylistWidget.cpp
    SampleWidget.cpp
    Skin.cpp
    SoundPlayerWidgetAdvancedView.cpp
)

serenity_app(SoundPlayer ICON app-sound-player)
target_link_libraries(SoundPlayer LibAudio LibGUI LibArchive LibCompress LibGfx)
