set(SOURCES
    main.cpp
)

serenity_app(Audio.Applet ICON audio-volume-high)
target_link_libraries(Audio.Applet LibGUI LibGfx LibAudio)
