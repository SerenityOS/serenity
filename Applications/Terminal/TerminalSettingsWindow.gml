@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::GroupBox {
        title: "Bell mode"
        fixed_height: 94

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
        fixed_height: 50

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
}
