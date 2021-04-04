@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout

        @GUI::Label {
            text: "Add title & date:"
            text_alignment: "CenterLeft"
            font_weight: "Bold"
        }

        @GUI::TextBox {
            name: "title_box"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout

        @GUI::ComboBox {
            name: "month_combo"
            model_only: true
        }

        @GUI::SpinBox {
            name: "day_combo"
            range: [1, 31]
        }

        @GUI::SpinBox {
            name: "year_combo"
            range: [0, 9999]
        }
    }

    @GUI::Widget {
        shrink_to_fit: true

        layout: @GUI::HorizontalBoxLayout

        @GUI::Widget

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            fixed_width: 80
        }
    }
}
