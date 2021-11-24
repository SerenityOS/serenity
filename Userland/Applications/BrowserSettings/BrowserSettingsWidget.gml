@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Homepage"
        fixed_height: 70

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
                fixed_width: 30
            }

            @GUI::TextBox {
                name: "homepage_url_textbox"
                placeholder: "https://example.com"
            }
        }
    }

    @GUI::GroupBox {
        title: "Downloads"
        fixed_height: 70

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
                name: "download_image_label"
            }

            @GUI::CheckBox {
                name: "auto_close_download_windows_checkbox"
                text: "Automatically close download window when complete"
            }
        }
    }
}
