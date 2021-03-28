set(SOURCES
    main.cpp
    GMLAutocompleteProvider.cpp
)

serenity_app(Playground ICON app-playground)
target_link_libraries(Playground LibDesktop LibGUI)
