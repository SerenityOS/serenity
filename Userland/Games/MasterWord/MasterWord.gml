@MasterWord::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @MasterWord::WordGame {
        name: "word_game"
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
