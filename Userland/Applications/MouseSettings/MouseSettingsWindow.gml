@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::GroupBox {
        title: "Mouse speed"
        fixed_height: 60

        layout: @GUI::HorizontalBoxLayout {
            margins: [6, 16, 8, 6]
        }

        @GUI::HorizontalSlider {
            name: "speed_slider"
            max: 3500
            min: 500
            value: 100
        }

        @GUI::Label {
            name: "speed_label"
            text: "100.0 %"
            fixed_width: 50
            text_alignment: "CenterRight"
        }
    }

    @GUI::GroupBox {
        title: "Scroll length"
        fixed_height: 60

        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 16, 8, 6]
        }

        @GUI::Label {
            text: "Scroll by "
            autosize: true
            text_alignment: "CenterLeft"
        }

        @GUI::SpinBox {
            name: "scroll_length_spinbox"
            max: 32
            min: 1
            value: 4
            text_alignment: "CenterRight"
            fixed_width: 80
        }

        @GUI::Label {
            text: " lines at a time"
            text_alignent: "CenterLeft"
            autosize: true
        }
    }

    @GUI::GroupBox {
        title: "Double-click speed"
        fixed_height: 60

        layout: @GUI::HorizontalBoxLayout {
            margins: [6, 16, 8, 6]
        }

        @GUI::HorizontalSlider {
            name: "double_click_speed_slider"
            max: 900
            min: 100
            value: 250
        }

        @GUI::Label {
            name: "double_click_speed_label"
            text: "250 ms"
            fixed_width: 50
            text_alignment: "CenterRight"
        }
    }

    @GUI::Widget {
        fixed_height: 22

        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::Button {
            name: "ok_button"
            text: "OK"
        }

        @GUI::Button {
            name: "apply_button"
            text: "Apply"
        }

        @GUI::Button {
            name: "reset_button"
            text: "Reset"
        }
    }
}
