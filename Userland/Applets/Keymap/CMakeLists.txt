serenity_component(
    Keymap.Applet
    REQUIRED
    TARGETS Keymap.Applet
)

set(SOURCES
    KeymapStatusWindow.cpp
    KeymapStatusWidget.cpp
    main.cpp
)

serenity_bin(Keymap.Applet)
target_link_libraries(Keymap.Applet PRIVATE LibGUI LibCore LibGfx LibIPC LibMain LibKeyboard)
