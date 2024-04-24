@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [2]
    }

    @GUI::Frame {
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }
        frame_style: "SunkenBox"

        @GUI::Label {
            name: "preview_label"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 22

        @GUI::TextBox {
            name: "preview_textbox"
            placeholder: "Preview text"
        }

        @GUI::Button {
            name: "reload_button"
            icon: "/res/icons/16x16/reload.png"
            fixed_width: 22
        }
    }
}
