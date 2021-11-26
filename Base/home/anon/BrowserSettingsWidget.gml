@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Homepage"
        fixed_height: 60

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                fixed_width: 32
                fixed_height: 32
                name: "homepage_image_label"
            }

            @GUI::Label {
                text: "URL:"
                text_alignment: "CenterLeft"
            }

            @GUI::TextBox {
                name: "homepage_url_textbox"
                placeholder: "https://example.com"
            }
        }
    }

    @GUI::GroupBox {
        title: "Search Engine"
        fixed_height: 100

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                fixed_width: 32
                fixed_height: 32
                name: "search_engine_image_label"
            }

            @GUI::Label {
                text: "Search engine:"
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "search_engine_combobox"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            name: "search_engine_custom_group"

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                text: "Enter URL template:"
                text_alignment: "CenterLeft"
            }

            @GUI::TextBox {
                name: "search_engine_custom_textbox"
                placeholder: "https://host/search?q={}"
            }
        }
    }
}
