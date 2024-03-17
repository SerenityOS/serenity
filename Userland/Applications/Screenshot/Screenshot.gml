@Screenshot::MainWidget {
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout {
        margins: [6, 4, 4, 4]
        spacing: 6
    }

    @GUI::Widget {
        max_width: 32
        layout: @GUI::VerticalBoxLayout {}

        @GUI::ImageWidget {
            bitmap: "/res/icons/32x32/app-screenshot.png"
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::GroupBox {
            title: "Take screenshot of:"
            layout: @GUI::VerticalBoxLayout {
                margins: [4]
                spacing: 0
            }

            @GUI::RadioButton {
                name: "whole_desktop"
                text: "Whole desktop"
                checked: true
            }

            @GUI::RadioButton {
                name: "selected_area"
                text: "Selected area"
            }
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                margins: [0, 0, 0, 8]
            }

            @GUI::CheckBox {
                name: "edit_in_pixel_paint"
                text: "Edit in Pixel Paint"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::TextBox {
                name: "destination"
                mode: "DisplayOnly"
            }

            @GUI::Button {
                name: "browse"
                max_width: 22
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Layout::Spacer {}

            @GUI::DialogButton {
                name: "ok_button"
                text: "OK"
            }

            @GUI::DialogButton {
                name: "cancel_button"
                text: "Cancel"
            }
        }
    }
}
