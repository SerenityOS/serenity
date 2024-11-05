@Chess::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Frame {
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Frame {
            name: "chess_widget_frame"
            min_width: 508
            min_height: 508
            layout: @GUI::HorizontalBoxLayout {}

            @Chess::ChessWidget {
                name: "chess_widget"
            }
        }

        @GUI::Frame {
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Label {
                text: "Moves"
                text_alignment: "Center"
                font_weight: "Bold"
                fixed_height: 24
                name: "moves_display_widget_label"
            }

            @GUI::TextEditor {
                name: "move_display_widget"
                mode: "ReadOnly"
            }

            @GUI::Label {
                text_alignment: "CenterLeft"
                font_weight: "Bold"
                autosize: true
                name: "white_time_label"
            }

            @GUI::Label {
                text_alignment: "CenterLeft"
                font_weight: "Bold"
                autosize: true
                name: "black_time_label"
            }
        }
    }
}
