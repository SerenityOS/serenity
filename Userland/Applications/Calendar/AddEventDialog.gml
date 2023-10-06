@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 45
        layout: @GUI::VerticalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "Add title & date:"
            text_alignment: "CenterLeft"
            fixed_height: 14
            font_weight: "Bold"
        }

        @GUI::TextBox {
            name: "event_title_textbox"
            fixed_height: 20
        }
    }

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 25
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::ComboBox {
            name: "start_month"
            model_only: true
            fixed_size: [50, 20]
        }

        @GUI::SpinBox {
            name: "start_day"
            fixed_size: [40, 20]
            min: 1
        }

        @GUI::SpinBox {
            name: "start_year"
            fixed_size: [55, 20]
            min: 0
            max: 9999
        }
    }

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 25
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::SpinBox {
            name: "start_hour"
            fixed_size: [50, 20]
            min: 1
            max: 12
        }

        @GUI::SpinBox {
            name: "start_minute"
            fixed_size: [40, 20]
            min: 1
            max: 59
        }

        @GUI::ComboBox {
            name: "start_meridiem"
            model_only: true
            fixed_size: [55, 20]
        }
    }

    @GUI::Layout::Spacer {}

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 20
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            fixed_size: [80, 20]
        }
    }
}
