serenity_component(
    BrowserSettings
    RECOMMENDED
    TARGETS BrowserSettings
)

stringify_gml(AutoplaySettingsWidget.gml AutoplaySettingsWidgetGML.h autoplay_settings_widget_gml)
stringify_gml(BrowserSettingsWidget.gml BrowserSettingsWidgetGML.h browser_settings_widget_gml)
stringify_gml(ContentFilterSettingsWidget.gml ContentFilterSettingsWidgetGML.h content_filter_settings_widget_gml)

set(SOURCES
    AutoplaySettingsWidget.cpp
    BrowserSettingsWidget.cpp
    ContentFilterSettingsWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    AutoplaySettingsWidgetGML.h
    BrowserSettingsWidgetGML.h
    ContentFilterSettingsWidgetGML.h
)

serenity_app(BrowserSettings ICON app-browser)
target_link_libraries(BrowserSettings PRIVATE LibCore LibGfx LibGUI LibConfig LibMain)
