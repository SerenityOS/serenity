set(SOURCES
    main.cpp
)

serenity_bin(Audio.MenuApplet)
target_link_libraries(Audio.MenuApplet LibGUI LibGfx LibAudio)
