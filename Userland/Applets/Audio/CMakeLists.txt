serenity_component(
    Audio.Applet
    REQUIRED
    TARGETS Audio.Applet
)

set(SOURCES
    main.cpp
)

serenity_app(Audio.Applet ICON audio-volume-high)
target_link_libraries(Audio.Applet PRIVATE LibGUI LibGfx LibAudio LibCore LibConfig LibIPC LibMain)
