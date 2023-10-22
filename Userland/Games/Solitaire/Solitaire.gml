@Solitaire::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @Solitaire::Game {
        name: "game"
        fill_with_background_color: true
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 3
    }
}
