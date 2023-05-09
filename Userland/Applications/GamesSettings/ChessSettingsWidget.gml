@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GamesSettings::ChessGamePreview {
        name: "chess_preview"
        fill_with_background_color: true
        fixed_height: 160
    }

    @GUI::GroupBox {
        title: "Appearance"
        max_height: "shrink"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Piece Set:"
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "piece_set"
                model_only: true
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Board Theme:"
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "board_theme"
                model_only: true
            }
        }

        @GUI::CheckBox {
            name: "show_coordinates"
            text: "Show coordinates"
            checkbox_position: "Right"
        }

        @GUI::CheckBox {
            name: "highlight_checks"
            text: "Highlight checks"
            checkbox_position: "Right"
        }
    }
}
