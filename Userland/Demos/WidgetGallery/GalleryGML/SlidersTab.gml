@GUI::Widget {
    name: "sliders_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::GroupBox {
            preferred_height: "fit"
            layout: @GUI::HorizontalBoxLayout {
                margins: [8]
            }

            @GUI::HorizontalOpacitySlider {
                name: "opacity_slider"
                tooltip: "Opacity Slider"
            }

            @GUI::VerticalSeparator {}

            @GUI::ValueSlider {
                name: "opacity_value_slider"
                min: 0
                max: 100
                value: 100
                tooltip: "Value Slider"
            }
        }

        @GUI::HorizontalSeparator {}

        @GUI::Frame {
            frame_style: "SunkenPanel"
            preferred_height: "fit"
            layout: @GUI::VerticalBoxLayout {
                margins: [1]
            }

            @GUI::ImageWidget {
                name: "opacity_imagewidget"
            }
        }
    }

    @GUI::Widget {
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [0, 8]
        }

        @GUI::Scrollbar {
            name: "enabled_scrollbar"
            fixed_height: 16
            min: 0
            max: 100
            value: 50
        }

        @GUI::HorizontalSeparator {}

        @GUI::Scrollbar {
            name: "disabled_scrollbar"
            fixed_height: 16
        }

        @GUI::Layout::Spacer {}
    }

    @GUI::GroupBox {
        layout: @GUI::HorizontalBoxLayout {
            margins: [6]
        }
        preferred_height: "opportunistic_grow"

        @GUI::Layout::Spacer {}

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

        @GUI::Layout::Spacer {}

        @GUI::VerticalSeparator {}

        @GUI::VerticalSlider {
            enabled: false
            tooltip: "Disabled"
            min: 0
            max: 10
            value: 5
        }

        @GUI::VerticalSeparator {}

        @GUI::Layout::Spacer {}

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

        @GUI::Layout::Spacer {}
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }
        preferred_height: "fit"

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}
            preferred_height: "fit"

            @GUI::HorizontalSlider {
                name: "horizontal_slider_left"
                knob_size_mode: "Fixed"
                min: 0
                max: 100
                value: 0
            }

            @GUI::VerticalSeparator {}

            @GUI::HorizontalSlider {
                enabled: false
                min: 0
                max: 10
                value: 5
            }

            @GUI::VerticalSeparator {}

            @GUI::HorizontalSlider {
                name: "horizontal_slider_right"
                knob_size_mode: "Proportional"
                min: 0
                max: 5
                value: 0
            }
        }

        @GUI::HorizontalSeparator {}

        @GUI::HorizontalProgressbar {
            name: "horizontal_progressbar"
            fixed_height: 20
        }
    }
}
