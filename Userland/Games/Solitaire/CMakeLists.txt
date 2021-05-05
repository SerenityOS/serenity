set(SOURCES
    Card.cpp
    CardStack.cpp
    Game.cpp
    main.cpp
)

serenity_app(Solitaire ICON app-solitaire)
target_link_libraries(Solitaire LibGUI LibGfx LibCore)
