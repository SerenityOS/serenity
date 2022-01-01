@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [10, 10]
        }

        @TicTacToe::Board {
            name: "board"
            fill_with_background_color: true
            background_color: "#333"
            fixed_height: 314
            fixed_width: 316

            layout: @GUI::VerticalBoxLayout {
                spacing: 6
            }

            @GUI::Widget {

                layout: @GUI::HorizontalBoxLayout {
                    spacing: 8
                }

                @TicTacToe::Cell {
                    name: "cell_0"
                    index: 0
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }

                @TicTacToe::Cell {
                    name: "cell_1"
                    index: 1
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }

                @TicTacToe::Cell {
                    name: "cell_2"
                    index: 2
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }
            }

            @GUI::Widget {

                layout: @GUI::HorizontalBoxLayout {
                    spacing: 8
                }

                @TicTacToe::Cell {
                    name: "cell_3"
                    index: 3
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }

                @TicTacToe::Cell {
                    name: "cell_4"
                    index: 4
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }

                @TicTacToe::Cell {
                    name: "cell_5"
                    index: 5
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }
            }

            @GUI::Widget {

                layout: @GUI::HorizontalBoxLayout {
                    spacing: 8
                }

                @TicTacToe::Cell {
                    name: "cell_6"
                    index: 6
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }

                @TicTacToe::Cell {
                    name: "cell_7"
                    index: 7
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }

                @TicTacToe::Cell {
                    name: "cell_8"
                    index: 8
                    fill_with_background_color: true

                    fixed_height: 100
                    fixed_width: 100
                }
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
        label_count: 3
    }
}
