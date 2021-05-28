@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::GroupBox {
        title: "Bell mode"
        shrink_to_fit: true

        layout: @GUI::VerticalBoxLayout {
            margins: [6, 16, 6, 6]
        }

        @GUI::RadioButton {
            name: "beep_bell_radio"
            text: "System beep"
        }

        @GUI::RadioButton {
            name: "visual_bell_radio"
            text: "Visual bell"
        }

        @GUI::RadioButton {
            name: "no_bell_radio"
            text: "No bell"
        }
    }

    @GUI::GroupBox {
        title: "Background opacity"
        shrink_to_fit: true

        layout: @GUI::VerticalBoxLayout {
            margins: [6, 16, 6, 6]
        }

        @GUI::OpacitySlider {
            name: "background_opacity_slider"
            min: 0
            max: 255
            orientation: "Horizontal"
        }
    }

    @GUI::GroupBox {
        title: "Scrollback size (lines)"
        shrink_to_fit: true

        layout: @GUI::VerticalBoxLayout {
            margins: [6, 16, 6, 6]
        }

        @GUI::SpinBox {
            name: "history_size_spinbox"
            min: 0
            max: 40960
            orientation: "Horizontal"
        }
    }

    @GUI::GroupBox {
        title: "Color scheme"
        shrink_to_fit: true

        layout: @GUI::VerticalBoxLayout {
            margins: [6, 16, 6, 6]
        }

        @GUI::ComboBox {
            name: "color_scheme_combo"
        }
    }
}
