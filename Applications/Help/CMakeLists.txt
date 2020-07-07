set(SOURCES
    History.cpp
    main.cpp
    ManualModel.cpp
    ManualPageNode.cpp
    ManualSectionNode.cpp
)

serenity_bin(Help)
target_link_libraries(Help LibWeb LibMarkdown LibGUI LibDesktop)
