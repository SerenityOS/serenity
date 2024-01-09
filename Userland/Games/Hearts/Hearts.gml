@Hearts::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @Hearts::Game {
        name: "game"
        fill_with_background_color: true
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
