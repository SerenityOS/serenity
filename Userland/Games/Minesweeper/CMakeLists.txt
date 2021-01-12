set(SOURCES
    Field.cpp
    main.cpp
)

serenity_app(Minesweeper ICON app-minesweeper)
target_link_libraries(Minesweeper LibGUI)
