set(SOURCES
    main.cpp
    AudioEngine.cpp
    WaveWidget.cpp
    KeysWidget.cpp
    MainWidget.cpp
    KnobsWidget.cpp
    RollWidget.cpp
)

serenity_app(Piano)
target_link_libraries(Piano LibAudio LibGUI)
