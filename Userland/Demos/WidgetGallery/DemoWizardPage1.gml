@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [20]
    }

    @GUI::Label {
        text: "Please select an installation directory."
        text_alignment: "TopLeft"
        fixed_height: 32
    }

    @GUI::Widget {
        fixed_height: 25
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Location: "
            autosize: true
        }

        @GUI::TextBox {
            name: "page_1_location_text_box"
        }

        @GUI::Button {
            text: "Browse"
            fixed_width: 75
        }
    }

    @GUI::Layout::Spacer {}
}
