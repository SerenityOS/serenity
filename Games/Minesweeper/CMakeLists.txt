set(SOURCES
    Field.cpp
    main.cpp
)

serenity_bin(Minesweeper)
target_link_libraries(Minesweeper LibGUI)
