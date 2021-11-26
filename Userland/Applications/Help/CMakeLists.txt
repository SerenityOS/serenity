serenity_component(
    Help
    RECOMMENDED
    TARGETS Help
)

set(SOURCES
    History.cpp
    main.cpp
    ManualModel.cpp
    ManualPageNode.cpp
    ManualSectionNode.cpp
)

serenity_app(Help ICON app-help)
target_link_libraries(Help LibWeb LibMarkdown LibGUI LibDesktop LibMain)
