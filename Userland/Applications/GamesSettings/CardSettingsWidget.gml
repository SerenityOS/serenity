@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::Frame {
        name: "cards_preview"
        max_height: "shrink"
        background_color: "green"
        fill_with_background_color: true
        layout: @GUI::HorizontalBoxLayout {
            margins: [8]
            spacing: 8
        }

        @GUI::Layout::Spacer {}

        @GUI::ImageWidget {
            name: "cards_preview_card_back"
        }

        @GUI::ImageWidget {
            name: "cards_preview_card_front_ace"
        }

        @GUI::ImageWidget {
            name: "cards_preview_card_front_queen"
        }

        @GUI::Layout::Spacer {}
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
