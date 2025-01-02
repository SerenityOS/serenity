serenity_component(
    Maps
    RECOMMENDED
    TARGETS Maps
)

compile_gml(FavoritesEditDialog.gml FavoritesEditDialogGML.cpp)
compile_gml(FavoritesPanel.gml FavoritesPanelGML.cpp)
compile_gml(SearchPanel.gml SearchPanelGML.cpp)

set(SOURCES
    main.cpp
    FavoritesEditDialogGML.cpp
    FavoritesModel.cpp
    FavoritesPanelGML.cpp
    FavoritesPanel.cpp
    SearchPanelGML.cpp
    SearchPanel.cpp
    UsersMapWidget.cpp
)

serenity_app(Maps ICON app-maps)
target_link_libraries(Maps PRIVATE LibConfig LibCore LibDesktop LibGfx LibGUI LibMain LibMaps LibProtocol LibURL)
