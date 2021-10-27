@GUI::Widget {
    layout: @GUI::VerticalBoxLayout
    fill_with_background_color: true

    @GUI::Frame {
        layout: @GUI::HorizontalBoxLayout
        name: "preview_frame"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout
        fixed_height: 20

        @GUI::ComboBox {
            name: "color_combo_box"
            model_only: true
        }

        @GUI::ColorInput {
            name: "color_input"
        }
    }
}
