serenity_component(
    Solitaire
    RECOMMENDED
    TARGETS Solitaire
)

compile_gml(Solitaire.gml SolitaireGML.cpp solitaire_gml)

set(SOURCES
    Game.cpp
    SolitaireGML.cpp
    main.cpp
)

serenity_app(Solitaire ICON app-solitaire)
target_link_libraries(Solitaire PRIVATE LibCards LibConfig LibGUI LibDesktop LibGfx LibCore LibMain LibURL)
