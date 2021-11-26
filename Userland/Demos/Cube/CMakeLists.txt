serenity_component(
    Cube
    TARGETS Cube
)

set(SOURCES
    Cube.cpp
)

serenity_app(Cube ICON app-cube)
target_link_libraries(Cube LibGUI LibMain)
