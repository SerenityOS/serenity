@GUI::AboutDialogWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 0
    }

    @GUI::ImageWidget {
        name: "brand_banner"
        bitmap: "/res/graphics/brand-banner.png"
    }

    @GUI::Widget {
        name: "content_container"
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Widget {
            name: "left_container"
            fixed_width: 60
            layout: @GUI::VerticalBoxLayout {
                margins: [12, 0, 0]
            }

            @GUI::Widget {
                name: "icon_wrapper"
                fixed_size: [32, 48]
                layout: @GUI::VerticalBoxLayout {}

                @GUI::ImageWidget {
                    name: "icon"
                }
            }
        }

        @GUI::Widget {
            name: "right_container"
            layout: @GUI::VerticalBoxLayout {
                margins: [12, 4, 4, 0]
            }

            @GUI::Label {
                name: "name"
                text_alignment: "CenterLeft"
                fixed_height: 14
                font_weight: "Bold"
            }

            @GUI::Label {
                name: "serenity_os"
                text_alignment: "CenterLeft"
                fixed_height: 14
                text: "SerenityOS"
            }

            @GUI::Label {
                name: "version"
                text_alignment: "CenterLeft"
                fixed_height: 14
            }

            @GUI::Label {
                name: "copyright"
                text_alignment: "CenterLeft"
                fixed_height: 14
                text: "Copyright © the SerenityOS developers, 2018-2024"
            }

            @GUI::Layout::Spacer {}

            @GUI::Widget {
                name: "button_container"
                fixed_height: 22
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Layout::Spacer {}

                @GUI::DialogButton {
                    name: "ok_button"
                    text: "OK"
                }
            }
        }
    }
}
