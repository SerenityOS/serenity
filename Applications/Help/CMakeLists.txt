set(SOURCES
    main.cpp
    History.cpp
    ManualModel.cpp
    ManualPageNode.cpp
    ManualSectionNode.cpp
)

serenity_app(Help)
target_link_libraries(Help LibHTML LibMarkdown)
