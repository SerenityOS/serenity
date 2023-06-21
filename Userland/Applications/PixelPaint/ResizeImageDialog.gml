@GUI::Widget {
    fill_with_background_color: true
    min_width: 260
    min_height: 280
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        title: "Size (px)"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}
            fixed_height: 24

            @GUI::Label {
                text: "Width:"
                fixed_width: 60
                text_alignment: "CenterRight"
            }

            @GUI::SpinBox {
                name: "width_spinbox"
                min: 1
                max: 16384
                min_width: 140
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}
            fixed_height: 24

            @GUI::Label {
                text: "Height:"
                fixed_width: 60
                text_alignment: "CenterRight"
            }

            @GUI::SpinBox {
                name: "height_spinbox"
                min: 1
                max: 16384
                min_width: 140
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}
            fixed_height: 24
            min_width: 140

            @GUI::Widget {
                fixed_width: 60
            }

            @GUI::CheckBox {
                name: "keep_aspect_ratio_checkbox"
                text: "Keep aspect ratio"
                checked: true
                autosize: true
            }
        }
    }

    @GUI::GroupBox {
        title: "Scaling Mode"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }

        @GUI::RadioButton {
            name: "nearest_neighbor_radio"
            text: "Nearest neighbor"
            checked: true
            autosize: true
        }

        @GUI::RadioButton {
            name: "smooth_pixels_radio"
            text: "Smooth Pixels"
            autosize: true
        }

        @GUI::RadioButton {
            name: "bilinear_radio"
            text: "Bilinear"
            autosize: true
        }

        @GUI::RadioButton {
            name: "box_sampling_radio"
            text: "Box Sampling"
            autosize: true
        }

        @GUI::RadioButton {
            name: "resize_canvas"
            text: "Resize Canvas (None)"
            autosize: true
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "ok_button"
            text: "OK"
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
