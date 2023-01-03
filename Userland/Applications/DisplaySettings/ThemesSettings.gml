@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [14, 14, 4]
        }
        title: "Window Theme"
        fixed_height: 294

        @GUI::Frame {
            layout: @GUI::HorizontalBoxLayout {}
            name: "preview_frame"
            fixed_width: 306
            fixed_height: 201
        }

        @GUI::Widget {
            fixed_height: 20
        }

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                text: "Theme:"
                text_alignment: "CenterLeft"
                preferred_width: 95
            }

            @GUI::ComboBox {
                name: "themes_combo"
            }
        }
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [14, 14, 4]
        }
        title: "Cursor Theme"
        shrink_to_fit: true

        @GUI::Button {
            name: "cursor_themes_button"
            text: "Change in Mouse Settings..."
            fixed_width: 200
        }

        @GUI::Widget {
            fixed_height: 10
        }
    }
}
