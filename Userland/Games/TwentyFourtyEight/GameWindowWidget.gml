@TwentyFourtyEight::GameWindowWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Widget {
            name: "board_view_container"
            layout: @GUI::VerticalBoxLayout {}
        }

        @GUI::Statusbar {
            name: "statusbar"
        }
    }
}
