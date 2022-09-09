serenity_component(
    Tubes
    TARGETS Tubes
)

set(SOURCES
        Shapes.cpp
        Tubes.cpp
        main.cpp
)

serenity_app(Tubes ICON app-tubes)
target_link_libraries(Tubes LibGUI LibCore LibGfx LibGL LibMain)
