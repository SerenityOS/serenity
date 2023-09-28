serenity_component(
    GamesSettings
    REQUIRED
    TARGETS GamesSettings
)

compile_gml(CardSettingsWidget.gml CardSettingsWidgetGML.cpp)
compile_gml(ChessSettingsWidget.gml ChessSettingsWidgetGML.cpp)

set(SOURCES
    main.cpp
    CardSettingsWidget.cpp
    CardSettingsWidgetGML.cpp
    ChessSettingsWidgetGML.cpp
    ChessSettingsWidget.cpp
)

serenity_app(GamesSettings ICON games)
target_link_libraries(GamesSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibCards LibChess)
