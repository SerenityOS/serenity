@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout {
        margins: [4]
        spacing: 3
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::VerticalBoxLayout {
            margins: [0, 4]
        }

        @GUI::Label {
            text: "Look in:"
            text_alignment: "CenterRight"
            fixed_height: 24
        }

        @GUI::Tray {
            name: "common_locations_tray"
            fixed_width: 95
        }

        @GUI::Label {
            text: "Filename:"
            text_alignment: "CenterRight"
            fixed_height: 24
        }

        @GUI::Widget {
            fixed_height: 20
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Widget {
            shrink_to_fit: true
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::TextBox {
                name: "location_textbox"
            }

            @GUI::Toolbar {
                name: "toolbar"
            }
        }

        @GUI::MultiView {
            name: "view"
        }

        @GUI::Widget {
            shrink_to_fit: true
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Widget {
                fixed_height: 22
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::TextBox {
                    name: "filename_textbox"
                }

                @GUI::Widget {
                    fixed_width: 20
                }

                @GUI::Button {
                    name: "ok_button"
                    text: "OK"
                    fixed_width: 75
                }
            }

            @GUI::Widget {
                fixed_height: 22
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Widget {}

                @GUI::Button {
                    name: "cancel_button"
                    text: "Cancel"
                    fixed_width: 75
                }
            }
        }
    }
}
