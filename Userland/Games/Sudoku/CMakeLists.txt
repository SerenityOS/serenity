serenity_component(
    Sudoku
    RECOMMENDED
    TARGETS Sudoku
)

set(SOURCES
    main.cpp
    Board.cpp
    Square.cpp
    SudokuWidget.cpp
)

serenity_app(Sudoku ICON app-sudoku)
target_link_libraries(Sudoku LibConfig LibGUI LibCore LibMain LibDesktop)
