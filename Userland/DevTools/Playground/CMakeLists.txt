serenity_component(
    Playground
    RECOMMENDED
    TARGETS Playground
)

set(SOURCES
    main.cpp
)

serenity_app(Playground ICON app-playground)
target_link_libraries(Playground LibDesktop LibGUI LibMain)
