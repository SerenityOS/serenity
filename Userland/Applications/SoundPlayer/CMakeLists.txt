set(SOURCES
    main.cpp
    PlaybackManager.cpp
    SampleWidget.cpp
    SoundPlayerWidget.cpp
    SoundPlayerWidgetAdvancedView.cpp
    BarsVisualizationWidget.cpp
    AudioAlgorithms.cpp
    NoVisualizationWidget.cpp
)

serenity_app(SoundPlayer ICON app-sound-player)
target_link_libraries(SoundPlayer LibAudio LibGUI)
