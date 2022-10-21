serenity_component(
    Starfield
    TARGETS Starfield
)

set(SOURCES
    Starfield.cpp
)

serenity_app(Starfield ICON app-starfield)
target_link_libraries(Starfield PRIVATE LibGUI LibCore LibGfx LibMain)
