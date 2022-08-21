serenity_component(
        GamesSettings
        REQUIRED
        TARGETS GamesSettings
)

compile_gml(CardSettingsWidget.gml CardSettingsWidgetGML.h card_settings_widget_gml)

set(SOURCES
        main.cpp
        CardSettingsWidgetGML.h
        CardSettingsWidget.cpp
        CardSettingsWidget.h
        )

serenity_app(GamesSettings ICON games)
target_link_libraries(GamesSettings LibGUI LibMain LibCards)
