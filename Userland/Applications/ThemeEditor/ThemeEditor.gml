@GUI::Widget {
    layout: @GUI::VerticalBoxLayout
    fill_with_background_color: true

    @GUI::Frame {
        layout: @GUI::HorizontalBoxLayout
        name: "preview_frame"
    }

    @GUI::GroupBox {
        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 8, 8, 8]
        }
        shrink_to_fit: true
        title: "Colors"

        @GUI::ComboBox {
            name: "color_combo_box"
            model_only: true
        }

        @GUI::ColorInput {
            name: "color_input"
        }
    }

    @GUI::GroupBox {
        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 8, 8, 8]
        }
        shrink_to_fit: true
        title: "Metrics"

        @GUI::ComboBox {
            name: "metric_combo_box"
            model_only: true
        }

        @GUI::SpinBox {
            name: "metric_input"
        }
    }
}
