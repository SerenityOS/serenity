serenity_component(
        GamesSettings
        REQUIRED
        TARGETS GamesSettings
)

compile_gml(CardSettingsWidget.gml CardSettingsWidgetGML.h card_settings_widget_gml)

set(SOURCES
    main.cpp
    CardSettingsWidget.cpp
)

set(GENERATED_SOURCES
    CardSettingsWidgetGML.h
)

serenity_app(GamesSettings ICON games)
target_link_libraries(GamesSettings LibGUI LibMain LibCards)
