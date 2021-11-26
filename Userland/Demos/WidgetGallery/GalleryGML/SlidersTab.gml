@GUI::Widget {
    name: "sliders_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        fixed_height: 129
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::GroupBox {
            max_height: 30

            layout: @GUI::HorizontalBoxLayout {
                margins: [8]
            }

            @GUI::OpacitySlider {
                name: "opacity_slider"
                tooltip: "Opacity Slider"
            }

            @GUI::VerticalSeparator {
            }

            @GUI::ValueSlider {
                name: "opacity_value_slider"
                min: 0
                max: 100
                value: 100
                tooltip: "Value Slider"
            }
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::Frame {
            shape: "Panel"
            shadow: "Sunken"
            thickness: 1
            max_width: 394
            max_height: 79
            layout: @GUI::VerticalBoxLayout {
                margins: [1]
            }

            @GUI::ImageWidget {
                name: "opacity_imagewidget"
            }
        }
    }

    @GUI::Widget {
        fixed_height: 88
        layout: @GUI::VerticalBoxLayout {
            margins: [0, 8]
        }

        @GUI::Widget {
        }

        @GUI::Scrollbar {
            name: "enabled_scrollbar"
            fixed_height: 16
            fixed_width: -1
            min: 0
            max: 100
            value: 50
        }

        @GUI::Widget {
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::Widget {
        }

        @GUI::Scrollbar {
            name: "disabled_scrollbar"
            fixed_height: 16
            fixed_width: -1
        }

        @GUI::Widget {
        }
    }

    @GUI::GroupBox {
        layout: @GUI::HorizontalBoxLayout {
            margins: [6]
        }

        @GUI::VerticalProgressbar {
            name: "vertical_progressbar_left"
            fixed_width: 36
        }

        @GUI::VerticalSlider {
            name: "vertical_slider_left"
            knob_size_mode: "Fixed"
            min: 0
            max: 100
            value: 100
            tooltip: "Fixed"
        }

        @GUI::VerticalSeparator {
        }

        @GUI::VerticalSlider {
            enabled: false
            tooltip: "Disabled"
            min: 0
            max: 10
            value: 5
        }

        @GUI::VerticalSeparator {
        }

        @GUI::VerticalProgressbar {
            name: "vertical_progressbar_right"
            fixed_width: 36
        }

        @GUI::VerticalSlider {
            name: "vertical_slider_right"
            knob_size_mode: "Proportional"
            min: 0
            max: 4
            value: 0
            tooltip: "Proportional"
        }
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::HorizontalSlider {
                name: "horizontal_slider_left"
                knob_size_mode: "Fixed"
                min: 0
                max: 100
                value: 0
            }

            @GUI::VerticalSeparator {
            }

            @GUI::HorizontalSlider {
                enabled: false
                min: 0
                max: 10
                value: 5
            }

            @GUI::VerticalSeparator {
            }

            @GUI::HorizontalSlider {
                name: "horizontal_slider_right"
                knob_size_mode: "Proportional"
                min: 0
                max: 5
                value: 0
            }
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::HorizontalProgressbar {
            name: "horizontal_progressbar"
            fixed_height: 20
        }
    }
}
