@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::Frame {
        layout: @GUI::HorizontalBoxLayout {}
        name: "preview_frame"
        fixed_width: 304
        fixed_height: 201
    }

    @GUI::Widget {
        fixed_height: 20
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Theme:"
            text_alignment: "CenterLeft"
            fixed_width: 95
        }

        @GUI::ComboBox {
            name: "themes_combo"
        }
    }
}
