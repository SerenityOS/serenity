@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            name: "time_format_widget"
            spacing: 10
        }

        @GUI::Label {
            text_alignment: "CenterRight"
            text: "Time Format:"
        }

        @GUI::ComboBox {
            name: "time_format_box"
            model_only: true
        }
    }
}
