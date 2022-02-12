@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @Solitaire::Game {
        name: "game"
        fill_with_background_color: true
        background_color: "green"
    }

    @GUI::Statusbar {
        name: "statusbar"
        label_count: 3
    }
}
