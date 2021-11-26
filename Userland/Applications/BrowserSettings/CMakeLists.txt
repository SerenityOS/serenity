serenity_component(
    BrowserSettings
    RECOMMENDED
    TARGETS BrowserSettings
)

compile_gml(BrowserSettingsWidget.gml BrowserSettingsWidgetGML.h browser_settings_widget_gml)

set(SOURCES
    main.cpp
    BrowserSettingsWidget.cpp
    BrowserSettingsWidgetGML.h
)

serenity_app(BrowserSettings ICON app-browser)
target_link_libraries(BrowserSettings LibGUI LibConfig LibMain)
