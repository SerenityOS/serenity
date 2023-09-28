@GamesSettings::CardSettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GamesSettings::CardGamePreview {
        name: "cards_preview"
        fill_with_background_color: true
        fixed_height: 160
    }

    @GUI::GroupBox {
        title: "Background color"
        max_height: "shrink"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::ColorInput {
            name: "cards_background_color"
            color_has_alpha_channel: false
        }
    }

    @GUI::GroupBox {
        title: "Cards"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::Widget {
            max_height: "shrink"
            layout: @GUI::HorizontalBoxLayout {
                margins: [0]
            }

            @GUI::Label {
                text: "Card fronts:"
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "cards_front_image_set"
                only_allow_values_from_model: true
            }
        }

        @GUI::Label {
            text: "Card backs:"
            text_alignment: "CenterLeft"
        }

        @GUI::IconView {
            name: "cards_back_image"
        }
    }
}
