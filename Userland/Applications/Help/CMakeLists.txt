serenity_component(
    Help
    RECOMMENDED
    TARGETS Help
    DEPENDS WebContent
)

compile_gml(HelpWindow.gml HelpWindowGML.h help_window_gml)

set(SOURCES
    HelpWindowGML.h
    History.cpp
    main.cpp
    MainWidget.cpp
    ManualModel.cpp
    ManualPageNode.cpp
    ManualSectionNode.cpp
)

serenity_app(Help ICON app-help)
target_link_libraries(Help LibWebView LibWeb LibMarkdown LibGUI LibDesktop LibMain)
