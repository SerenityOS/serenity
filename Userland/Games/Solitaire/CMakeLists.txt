serenity_component(
    Solitaire
    RECOMMENDED
    TARGETS Solitaire
)

stringify_gml(Solitaire.gml SolitaireGML.h solitaire_gml)

set(SOURCES
    Game.cpp
    main.cpp
)

set(GENERATED_SOURCES
    SolitaireGML.h
)

serenity_app(Solitaire ICON app-solitaire)
target_link_libraries(Solitaire PRIVATE LibCards LibConfig LibGUI LibDesktop LibGfx LibCore LibMain)
