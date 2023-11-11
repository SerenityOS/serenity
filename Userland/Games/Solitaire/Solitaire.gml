@Solitaire::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @Solitaire::Game {
        name: "game"
        fill_with_background_color: true
    }

    @GUI::Frame {
        name: "game_action_bar"
        fill_with_background_color: true
        fixed_height: 32
        layout: @GUI::HorizontalBoxLayout {
            margins: [3]
        }

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "solve_button"
            text: "Solve"
            fixed_width: 80
        }

        @GUI::Layout::Spacer {}
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 3
    }
}
