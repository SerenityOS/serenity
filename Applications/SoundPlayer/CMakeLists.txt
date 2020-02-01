set(SOURCES
    main.cpp
    PlaybackManager.cpp
    SampleWidget.cpp
    SoundPlayerWidget.cpp
)

serenity_app(SoundPlayer)
target_link_libraries(SoundPlayer LibAudio LibGUI)
