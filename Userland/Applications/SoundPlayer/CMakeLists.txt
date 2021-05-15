set(SOURCES
    main.cpp
    PlaybackManager.cpp
    SampleWidget.cpp
    SoundPlayerWidgetAdvancedView.cpp
    BarsVisualizationWidget.cpp
    AudioAlgorithms.cpp
    NoVisualizationWidget.cpp
    M3UParser.cpp
    PlaylistWidget.cpp
)

serenity_app(SoundPlayer ICON app-sound-player)
target_link_libraries(SoundPlayer LibAudio LibGUI)
