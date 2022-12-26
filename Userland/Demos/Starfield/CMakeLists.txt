serenity_component(
    Starfield
    TARGETS Starfield
)

set(SOURCES
    Starfield.cpp
)

serenity_app(Starfield ICON app-starfield)
target_link_libraries(Starfield PRIVATE LibDesktop LibGUI LibCore LibGfx LibMain)
