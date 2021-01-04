set(SOURCES
    main.cpp
)

serenity_app(Audio.MenuApplet ICON audio-volume-high)
target_link_libraries(Audio.MenuApplet LibGUI LibGfx LibAudio)
