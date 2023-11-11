@Snake::MainWidget {
    layout: @GUI::VerticalBoxLayout {}
    fill_with_background_color: true

    @Snake::Game {
        name: "game"
        fill_with_background_color: true
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 2
    }
}
