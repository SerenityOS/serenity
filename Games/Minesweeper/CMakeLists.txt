set(SOURCES
    main.cpp
    Field.cpp
)

serenity_app(Minesweeper)
target_link_libraries(Minesweeper LibGUI)
