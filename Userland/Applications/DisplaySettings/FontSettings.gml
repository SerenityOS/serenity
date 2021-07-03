@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [8, 8, 8, 8]
        spacing: 8
    }

    @GUI::Widget {
        shrink_to_fit: true

        layout: @GUI::HorizontalBoxLayout {
            spacing: 6
        }

        @GUI::Label {
            fixed_width: 100
            text: "Default font:"
            text_alignment: "CenterLeft"
        }

        @GUI::Frame {
            background_color: "white"
            fill_with_background_color: true

            layout: @GUI::VerticalBoxLayout {
            }

            @GUI::Label {
                name: "default_font_label"
                text: "Katica 10 400"
            }
        }

        @GUI::Button {
            text: "..."
            name: "default_font_button"
            fixed_width: 30
        }
    }

    @GUI::Widget {
        shrink_to_fit: true

        layout: @GUI::HorizontalBoxLayout {
            spacing: 6
        }

        @GUI::Label {
            fixed_width: 100
            text: "Fixed-width font:"
            text_alignment: "CenterLeft"
        }

        @GUI::Frame {
            background_color: "white"
            fill_with_background_color: true

            layout: @GUI::VerticalBoxLayout {
            }

            @GUI::Label {
                name: "fixed_width_font_label"
                text: "Csilla 10 400"
            }
        }

        @GUI::Button {
            text: "..."
            name: "fixed_width_font_button"
            fixed_width: 30
        }
    }

    @GUI::Widget {
    }
}
