serenity_component(
    SoundPlayer
    RECOMMENDED
    TARGETS SoundPlayer
    DEPENDS AudioServer
)

set(SOURCES
    main.cpp
    Player.cpp
    Playlist.cpp
    PlaybackManager.cpp
    SampleWidget.cpp
    SoundPlayerWidgetAdvancedView.cpp
    BarsVisualizationWidget.cpp
    NoVisualizationWidget.cpp
    M3UParser.cpp
    PlaylistWidget.cpp
)

serenity_app(SoundPlayer ICON app-sound-player)
target_link_libraries(SoundPlayer LibAudio LibDSP LibGUI LibMain)
