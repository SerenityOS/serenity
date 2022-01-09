serenity_component(
    Chess
    RECOMMENDED
    TARGETS Chess
    DEPENDS ChessEngine
)

set(SOURCES
    main.cpp
    ChessWidget.cpp
    PromotionDialog.cpp
    Engine.cpp
)

serenity_app(Chess ICON app-chess)
target_link_libraries(Chess LibChess LibConfig LibGUI LibCore LibMain LibDesktop)
