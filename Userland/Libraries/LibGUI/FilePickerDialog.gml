@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::HorizontalBoxLayout {
        margins: [4, 4, 4, 4]
        spacing: 3
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::VerticalBoxLayout {
            margins: [4, 4, 4, 4]
        }

        @GUI::Label {
            text: "Look in:"
            text_alignment: "CenterRight"
            fixed_height: 20
        }

        @GUI::Frame {
            name: "common_locations_frame"
            fixed_width: 90
            fill_with_background_color: true

            layout: @GUI::VerticalBoxLayout {
                margins: [2, 4, 2, 4]
                spacing: 0
            }
        }

        @GUI::Widget {
            fixed_height: 47
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout

        @GUI::Widget {
            shrink_to_fit: true
            layout: @GUI::HorizontalBoxLayout

            @GUI::TextBox {
                name: "location_textbox"
            }

            @GUI::ToolBar {
                name: "toolbar"
            }
        }

        @GUI::MultiView {
            name: "view"
        }

        @GUI::Widget {
            shrink_to_fit: true
            layout: @GUI::VerticalBoxLayout

            @GUI::Widget {
                fixed_height: 24
                layout: @GUI::HorizontalBoxLayout

                @GUI::Label {
                    text: "File name:"
                    text_alignment: "CenterLeft"
                    fixed_width: 80
                }

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
                fixed_height: 24
                layout: @GUI::HorizontalBoxLayout

                @GUI::Widget

                @GUI::Button {
                    name: "cancel_button"
                    text: "Cancel"
                    fixed_width: 75
                }
            }
        }
    }
}
