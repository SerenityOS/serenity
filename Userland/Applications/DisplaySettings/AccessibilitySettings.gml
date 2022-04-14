@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 8
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            spacing: 6
            margins: [8]
        }
        title: "Color Filters"
        fixed_height: 260
        

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                spacing: 3
                margins: [8]
            }

            @GUI::RadioButton {
                name: "filter_none_radio_button"
                text: "No filter"
                checked: true
                fixed_height: 16
            }

            @GUI::RadioButton {
                name: "filter_protanopia_radio_button"
                text: "Protanopia"
            }

            @GUI::RadioButton {
                name: "filter_protanomaly_radio_button"
                text: "Protanomaly"
            }

            @GUI::RadioButton {
                name: "filter_deuteranopia_radio_button"
                text: "Deuteranopia"
            }

            @GUI::RadioButton {
                name: "filter_deuteranomaly_radio_button"
                text: "Deuteranomaly"
            }

            @GUI::RadioButton {
                name: "filter_tritanopia_radio_button"
                text: "Tritanopia"
            }

            @GUI::RadioButton {
                name: "filter_tritanomaly_radio_button"
                text: "Tritanomaly"
            }

            @GUI::RadioButton {
                name: "filter_achromatopsia_radio_button"
                text: "Achromatopsia"
            }

            @GUI::RadioButton {
                name: "filter_achromatomaly_radio_button"
                text: "Achromatomaly"
            }
        }
    }

    @GUI::ImageWidget {
        name: "color_wheel_image"
        auto_resize: false
    }
}
