serenity_component(
    BrowserSettings
    RECOMMENDED
    TARGETS BrowserSettings
)

compile_gml(AutoplaySettingsWidget.gml AutoplaySettingsWidgetGML.cpp)
compile_gml(BrowserSettingsWidget.gml BrowserSettingsWidgetGML.cpp)
compile_gml(ContentFilterSettingsWidget.gml ContentFilterSettingsWidgetGML.cpp)

set(SOURCES
    AutoplaySettingsWidget.cpp
    BrowserSettingsWidget.cpp
    ContentFilterSettingsWidget.cpp
    AutoplaySettingsWidgetGML.cpp
    BrowserSettingsWidgetGML.cpp
    ContentFilterSettingsWidgetGML.cpp
    main.cpp
)

serenity_app(BrowserSettings ICON app-browser)
target_link_libraries(BrowserSettings PRIVATE LibCore LibGfx LibGUI LibConfig LibMain LibWebView LibURL)
