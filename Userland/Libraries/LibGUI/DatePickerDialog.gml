@GUI::Widget {
    shrink_to_fit: true
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Widget {
        fixed_height: 20
        fill_with_background_color: true
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::ComboBox {
            name: "month_box"
            model_only: true
        }

        @GUI::SpinBox {
            name: "year_box"
            fixed_size: [55, 20]
            min: 0
            max: 9999
        }

        @GUI::Layout::Spacer {}
    }

    @GUI::Widget {
        fixed_width: 200
        fixed_height: 200
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Calendar {
            name: "calendar_view"
            mode: "Month"
        }
    }

    @GUI::Widget {
        fixed_height: 20
        fill_with_background_color: true
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            fixed_size: [80, 20]
        }

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            fixed_size: [80, 20]
            default: true
        }
    }
}
