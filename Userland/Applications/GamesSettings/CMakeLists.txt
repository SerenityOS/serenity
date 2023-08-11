serenity_component(
    GamesSettings
    REQUIRED
    TARGETS GamesSettings
)

stringify_gml(CardSettingsWidget.gml CardSettingsWidgetGML.h card_settings_widget_gml)
stringify_gml(ChessSettingsWidget.gml ChessSettingsWidgetGML.h chess_settings_widget_gml)

set(SOURCES
    main.cpp
    CardSettingsWidget.cpp
    ChessSettingsWidget.cpp
)

set(GENERATED_SOURCES
    CardSettingsWidgetGML.h
    ChessSettingsWidgetGML.h
)

serenity_app(GamesSettings ICON games)
target_link_libraries(GamesSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain LibCards LibChess)
