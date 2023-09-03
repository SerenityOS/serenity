serenity_component(
    Maps
    REQUIRED
    TARGETS Maps
)

set(SOURCES
    main.cpp
    MapWidget.cpp
)

serenity_app(Maps ICON app-hello-world) # FIXME: Create Maps icon
target_link_libraries(Maps PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibProtocol)
