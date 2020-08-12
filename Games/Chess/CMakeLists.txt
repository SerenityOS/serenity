set(SOURCES
    main.cpp
    Chess.cpp
    ChessWidget.cpp
    PromotionDialog.cpp
)

serenity_bin(Chess)
target_link_libraries(Chess LibGUI)
