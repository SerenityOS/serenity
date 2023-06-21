serenity_component(
    Gradient
    TARGETS Gradient
)

set(SOURCES
    Gradient.cpp
)

serenity_app(Gradient ICON app-gradient)
target_link_libraries(Gradient PRIVATE LibDesktop LibGUI LibCore LibGfx LibMain)
