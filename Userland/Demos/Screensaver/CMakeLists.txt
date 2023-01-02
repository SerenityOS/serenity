serenity_component(
    Screensaver
    TARGETS Screensaver
)

set(SOURCES
    main.cpp
)

serenity_app(Screensaver ICON app-screensaver)
target_link_libraries(Screensaver PRIVATE LibDesktop LibGUI LibCore LibGfx LibMain)
