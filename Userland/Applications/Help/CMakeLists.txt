serenity_component(
    Help
    RECOMMENDED
    TARGETS Help
    DEPENDS WebContent
)

compile_gml(HelpWindow.gml HelpWindowGML.cpp)

set(SOURCES
    History.cpp
    main.cpp
    MainWidget.cpp
    ManualModel.cpp
    HelpWindowGML.cpp
)

serenity_app(Help ICON app-help)
target_link_libraries(Help PRIVATE LibCore LibWebView LibWeb LibMarkdown LibGfx LibGUI LibDesktop LibMain LibManual LibLocale LibURL)
