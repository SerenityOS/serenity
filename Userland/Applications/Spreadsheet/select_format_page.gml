@GUI::Widget {
    name: "select_format"
    layout: @GUI::VerticalBoxLayout {
        margins: [20]
    }

    @GUI::Label {
        text: "Please double-check the guessed file type\nor select the correct one below"
        text_alignment: "TopLeft"
        fixed_height: 32
    }

    @GUI::Widget {
        fixed_height: 25
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Type: "
            autosize: true
        }

        @GUI::ComboBox {
            name: "select_format_page_format_combo_box"
            model_only: true
        }
    }

    @GUI::Layout::Spacer {}
}
