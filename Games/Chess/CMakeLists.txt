set(SOURCES
    main.cpp
    ChessWidget.cpp
    PromotionDialog.cpp
    Engine.cpp
)

serenity_app(Chess ICON app-chess)
target_link_libraries(Chess LibChess LibGUI LibCore)
