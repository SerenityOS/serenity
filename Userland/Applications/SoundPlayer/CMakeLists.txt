set(SOURCES
    main.cpp
    PlaybackManager.cpp
    SampleWidget.cpp
    SoundPlayerWidget.cpp
)

serenity_app(SoundPlayer ICON app-sound-player)
target_link_libraries(SoundPlayer LibAudio LibGUI)
