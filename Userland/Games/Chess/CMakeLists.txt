serenity_component(
    Chess
    RECOMMENDED
    TARGETS Chess
    DEPENDS ChessEngine
)

compile_gml(Chess.gml ChessGML.cpp chess_gml)
compile_gml(PromotionWidget.gml PromotionWidgetGML.cpp promotionWidget_gml)
compile_gml(NewGameWidget.gml NewGameWidgetGML.cpp newGameWidget_gml)

set(SOURCES
    main.cpp
    ChessWidget.cpp
    PromotionDialog.cpp
    NewGameDialog.cpp
    Engine.cpp
    ChessGML.cpp
    PromotionWidgetGML.cpp
    NewGameWidgetGML.cpp
)

serenity_app(Chess ICON app-chess)
target_link_libraries(Chess PRIVATE LibChess LibConfig LibFileSystemAccessClient LibGfx LibGUI LibCore LibMain LibDesktop LibURL)
