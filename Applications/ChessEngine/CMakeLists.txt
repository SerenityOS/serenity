set(SOURCES
    ChessEngine.cpp
    main.cpp
    MCTSTree.cpp
)

serenity_bin(ChessEngine)
target_link_libraries(ChessEngine LibChess LibCore)
