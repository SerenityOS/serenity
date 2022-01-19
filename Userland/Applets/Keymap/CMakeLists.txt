serenity_component(
    Keymap.Applet
    REQUIRED
    TARGETS Keymap.Applet
)

set(SOURCES
    KeymapStatusWindow.cpp
    main.cpp
)

serenity_bin(Keymap.Applet)
target_link_libraries(Keymap.Applet LibGUI LibCore LibGfx LibMain LibKeyboard)
