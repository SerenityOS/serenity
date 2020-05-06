set(SOURCES
    Card.cpp
    CardStack.cpp
    main.cpp
    SolitaireWidget.cpp
)

serenity_bin(Solitaire)
target_link_libraries(Solitaire LibGUI LibGfx LibCore)
