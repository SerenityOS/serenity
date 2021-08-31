@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout {
        margins: [8, 8, 8, 8]
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
        }

        @GUI::Label {
            text: "Region to capture"
            fixed_height: 20
        }

        @GUI::RadioButton {
            name: "wholescreen"
            text: "Whole screen"
            checked: true
        }
        
        @GUI::RadioButton {
            name: "customregion"
            text: "Custom region"
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
        }

        @GUI::Label {
            text: "Delay before capturing"
            fixed_height: 20
        }

        @GUI::Widget {
            shrink_to_fit: true

            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::SpinBox {
                name: "delay"
                min: 0
                max: 60000
                value: 1000
            }

            @GUI::Label {
                text: "Miliseconds"
            }
        }

        @GUI::CheckBox {
            name: "copy_to_clipboard"
            text: "Copy to clipboard"            
            shrink_to_fit: true
        }

        @GUI::Widget

        @GUI::Widget {
            shrink_to_fit: true

            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Button {
                name: "ok_button"
                text: "OK"
            }
            
            @GUI::Button {
                name: "cancel_button"
                text: "Cancel"
            }
        }
    }
}
