@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        spacing: 0
    }

    @MasterWord::WordGame {
        name: "word_game"
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
