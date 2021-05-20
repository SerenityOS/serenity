compile_gml(Solitaire.gml SolitaireGML.h solitaire_gml)

set(SOURCES
    Game.cpp
    main.cpp
    SolitaireGML.h
)

serenity_app(Solitaire ICON app-solitaire)
target_link_libraries(Solitaire LibCards LibGUI LibGfx LibCore)
