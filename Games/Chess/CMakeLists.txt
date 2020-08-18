set(SOURCES
    main.cpp
    ChessWidget.cpp
    PromotionDialog.cpp
)

serenity_bin(Chess)
target_link_libraries(Chess LibChess LibGUI)
