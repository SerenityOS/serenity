set(SOURCES
    main.cpp
    PlaybackManager.cpp
    SampleWidget.cpp
    SoundPlayerWidget.cpp
)

serenity_bin(SoundPlayer)
target_link_libraries(SoundPlayer LibAudio LibGUI)
