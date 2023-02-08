serenity_component(
    Help
    RECOMMENDED
    TARGETS Help
    DEPENDS WebContent
)

stringify_gml(HelpWindow.gml HelpWindowGML.h help_window_gml)

set(SOURCES
    History.cpp
    main.cpp
    MainWidget.cpp
    ManualModel.cpp
)

set(GENERATED_SOURCES
    HelpWindowGML.h
)

serenity_app(Help ICON app-help)
target_link_libraries(Help PRIVATE LibCore LibWebView LibWeb LibMarkdown LibGfx LibGUI LibDesktop LibMain LibManual LibLocale)
link_with_locale_data(Help)
