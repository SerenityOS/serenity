@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GamesSettings::Preview {
        name: "cards_preview"
        fill_with_background_color: true
        fixed_height: 160
    }

    @GUI::GroupBox {
        title: "Background Color"
        max_height: "shrink"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::ColorInput {
            name: "cards_background_color"
            has_alpha_channel: false
        }
    }

    @GUI::GroupBox {
        title: "Card Back"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::IconView {
            name: "cards_back_image"
        }
    }
}
