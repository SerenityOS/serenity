set(SOURCES
    main.cpp
)

serenity_app(Audio.MenuApplet)
target_link_libraries(Audio.MenuApplet LibGUI LibAudio)
