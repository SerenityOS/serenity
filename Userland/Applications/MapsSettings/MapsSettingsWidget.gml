@MapsSettings::MapsSettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Tile Provider"
        fixed_height: 104
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/icons/32x32/search-engine.png"
            }

            @GUI::Label {
                text: "Tile Provider:"
                text_alignment: "CenterLeft"
                fixed_width: 110
            }

            @GUI::ComboBox {
                name: "tile_provider_combobox"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }
            name: "custom_tile_provider_group"

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                text: "Enter URL template:"
                text_alignment: "CenterLeft"
                fixed_width: 110
            }

            @GUI::TextBox {
                name: "custom_tile_provider_textbox"
            }
        }
    }
}
