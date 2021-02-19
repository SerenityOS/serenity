@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::Widget {
        shrink_to_fit: true

        layout: @GUI::HorizontalBoxLayout {
        }

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

        layout: @GUI::VerticalBoxLayout {
        }

        @GUI::Widget {
            fixed_height: 24
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Label {
                text: "File name:"
                text_alignment: "CenterLeft"
                fixed_width:80
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

            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Widget {
            }

            @GUI::Button {
                name: "cancel_button"
                text: "Cancel"
                fixed_width: 75
            }
        }
    }
}
