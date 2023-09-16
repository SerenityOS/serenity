serenity_component(
    Maps
    REQUIRED
    TARGETS Maps
)

compile_gml(SearchPanel.gml SearchPanelGML.cpp)

set(SOURCES
    main.cpp
    MapWidget.cpp
    SearchPanelGML.cpp
    SearchPanel.cpp
    UsersMapWidget.cpp
)

serenity_app(Maps ICON app-maps)
target_link_libraries(Maps PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibProtocol)
