@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @Hearts::Game {
        name: "game"
        fill_with_background_color: true
        background_color: "green"
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
