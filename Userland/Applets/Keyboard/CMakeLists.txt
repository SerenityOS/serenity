serenity_component(
    Keyboard.Applet
    REQUIRED
    TARGETS Keyboard.Applet
)

set(SOURCES
    main.cpp
)

serenity_app(Keyboard.Applet ICON app-keyboard-settings)
target_link_libraries(Keyboard.Applet LibGUI LibCore LibGfx)
