set(SOURCES
    main.cpp
    ChessWidget.cpp
    PromotionDialog.cpp
    Engine.cpp
)

serenity_bin(Chess)
target_link_libraries(Chess LibChess LibGUI)
