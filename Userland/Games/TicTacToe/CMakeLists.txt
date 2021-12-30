serenity_component(
    TicTacToe
    RECOMMENDED
    TARGETS TicTacToe
)
compile_gml(TicTacToe.gml TicTacToeGML.h tictactoe_gml)

set(SOURCES
    main.cpp
    Game.cpp
    Board.cpp
    Cell.cpp
    TicTacToeGML.h
)

serenity_app(TicTacToe ICON app-tictactoe)
target_link_libraries(TicTacToe LibConfig LibGUI LibMain)
