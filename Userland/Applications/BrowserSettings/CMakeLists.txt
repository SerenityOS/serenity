serenity_component(
    BrowserSettings
    RECOMMENDED
    TARGETS BrowserSettings
)

compile_gml(BrowserSettingsWidget.gml BrowserSettingsWidgetGML.h browser_settings_widget_gml)
compile_gml(ContentFilterSettingsWidget.gml ContentFilterSettingsWidgetGML.h content_filter_settings_widget_gml)

set(SOURCES
    BrowserSettingsWidget.cpp
    ContentFilterSettingsWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    BrowserSettingsWidgetGML.h
    ContentFilterSettingsWidgetGML.h
)

serenity_app(BrowserSettings ICON app-browser)
target_link_libraries(BrowserSettings LibGUI LibConfig LibMain)
