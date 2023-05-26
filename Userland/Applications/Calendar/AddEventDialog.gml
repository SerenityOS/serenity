@Calendar::AddEventWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 20
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Title:"
            text_alignment: "CenterLeft"
            fixed_height: 14
            font_weight: "Bold"
        }

        @GUI::TextBox {
            name: "event_title_textbox"
            fixed_size: [310, 20]
        }
    }

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 25
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "Start:"
            text_alignment: "CenterLeft"
            fixed_height: 14
            font_weight: "Bold"
        }

        @GUI::TextBox {
            name: "start_date"
            mode: "ReadOnly"
            fixed_width: 80
        }

        @GUI::Button {
            name: "pick_start_date"
            fixed_width: 20
        }

        @GUI::SpinBox {
            name: "start_hour"
            fixed_size: [50, 20]
            min: 0
            max: 23
        }

        @GUI::SpinBox {
            name: "start_minute"
            fixed_size: [40, 20]
            min: 0
            max: 59
        }
    }

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 25
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "End:"
            text_alignment: "CenterLeft"
            fixed_height: 14
            font_weight: "Bold"
        }

        @GUI::TextBox {
            name: "end_date"
            mode: "ReadOnly"
            fixed_width: 80
        }

        @GUI::Button {
            name: "pick_end_date"
            fixed_width: 20
        }

        @GUI::SpinBox {
            name: "end_hour"
            fixed_size: [50, 20]
            min: 0
            max: 23
        }

        @GUI::SpinBox {
            name: "end_minute"
            fixed_size: [40, 20]
            min: 0
            max: 59
        }
    }

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 25
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "Duration:"
            text_alignment: "CenterLeft"
            fixed_height: 14
            font_weight: "Bold"
        }

        @GUI::SpinBox {
            name: "duration_hour"
            fixed_size: [50, 20]
            min: 0
        }

        @GUI::SpinBox {
            name: "duration_minute"
            fixed_size: [40, 20]
            min: 0
            max: 59
        }
    }

    @GUI::Layout::Spacer {}

    @GUI::Widget {
        fill_with_background_color: true
        fixed_height: 20
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
        }
    }
}
