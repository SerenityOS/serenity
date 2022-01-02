@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Taskbar Appearance"
        shrink_to_fit: false
        fixed_height: 85

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::Label {
            text: "Location of taskbar on screen"
            text_alignment: "TopLeft"
        }

        @GUI::Widget {
            shrink_to_fit: true

            layout: @GUI::HorizontalBoxLayout {
                spacing: 4
            }

            @GUI::RadioButton {
                name: "taskbar_bottom_radio"
                text: "Bottom"
            }

            @GUI::RadioButton {
                name: "taskbar_left_radio"
                text: "Left"
            }

            @GUI::RadioButton {
                name: "taskbar_top_radio"
                text: "Top"
            }

            @GUI::RadioButton {
                name: "taskbar_right_radio"
                text: "Right"
            }
        }
    }
}
