@MouseSettings::MouseWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Cursor speed"
        fixed_height: 106
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/graphics/mouse-cursor-speed.png"
            }

            @GUI::Label {
                text: "The relative speed of the mouse cursor."
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::HorizontalSlider {
                name: "speed_slider"
                min: 0
                max: 100
                value: 50
            }

            @GUI::Label {
                fixed_width: 40
                name: "speed_label"
            }
        }
    }

    @GUI::GroupBox {
        title: "Scroll wheel"
        fixed_height: 106
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/graphics/scroll-wheel-step-size.png"
            }

            @GUI::Label {
                text: "The number of steps taken when the scroll wheel is\nmoved a single notch."
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                margins: [4, 0, 2, 8]
                spacing: 8
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                autosize: true
                text: "Step size:"
            }

            @GUI::SpinBox {
                name: "scroll_length_spinbox"
                min: 0
                max: 100
                value: 50
                preferred_width: "opportunistic_grow"
            }

            @GUI::Widget {
                fixed_width: 16
            }

            @GUI::CheckBox {
                name: "natural_scroll_checkbox"
                text: "Natural scrolling"
                tooltip: "Content follows motion instead of the viewport,\nalso commonly referred to as \"reverse scrolling\"."
                preferred_width: 110
            }
        }
    }

    @GUI::GroupBox {
        title: "Double-click speed"
        fixed_height: 106
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @MouseSettings::DoubleClickArrowWidget {
                fixed_width: 32
                fixed_height: 32
                name: "double_click_arrow_widget"
            }

            @GUI::Label {
                text: "The maximum time that may pass between two clicks\nin order for them to become a double-click."
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                margins: [8]
                spacing: 8
            }

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::HorizontalSlider {
                name: "double_click_speed_slider"
                min: 0
                max: 100
                value: 50
            }

            @GUI::Label {
                fixed_width: 40
                name: "double_click_speed_label"
            }
        }
    }

    @GUI::GroupBox {
        title: "Button configuration"
        fixed_height: 68
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                name: "switch_buttons_image"
            }

            @GUI::CheckBox {
                name: "switch_buttons_checkbox"
                text: "Switch primary and secondary buttons"
            }
        }
    }
}
