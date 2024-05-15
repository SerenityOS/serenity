serenity_component(
    Chess
    RECOMMENDED
    TARGETS Chess
    DEPENDS ChessEngine
)

compile_gml(Chess.gml ChessGML.cpp chess_gml)
compile_gml(PromotionWidget.gml PromotionWidgetGML.cpp promotionWidget_gml)

set(SOURCES
    main.cpp
    ChessWidget.cpp
    PromotionDialog.cpp
    Engine.cpp
    ChessGML.cpp
    PromotionWidgetGML.cpp
)

serenity_app(Chess ICON app-chess)
target_link_libraries(Chess PRIVATE LibChess LibConfig LibFileSystemAccessClient LibGfx LibGUI LibCore LibMain LibDesktop LibURL)
