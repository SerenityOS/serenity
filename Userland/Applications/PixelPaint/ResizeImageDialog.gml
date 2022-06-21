@GUI::Widget {
    fill_with_background_color: true
    min_width: 260
    min_height: 210
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        title: "Size (px)"
        shrink_to_fit: true
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
        shrink_to_fit: true
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
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Widget {}

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            max_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            max_width: 75
        }
    }
}
