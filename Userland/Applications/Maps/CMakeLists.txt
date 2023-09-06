serenity_component(
    Maps
    REQUIRED
    TARGETS Maps
)

set(SOURCES
    main.cpp
    MapWidget.cpp
    UsersMapWidget.cpp
)

serenity_app(Maps ICON app-maps)
target_link_libraries(Maps PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibProtocol)
