@Chess::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @Chess::ChessWidget {
        name: "chess_widget"
    }
}
