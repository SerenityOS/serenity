@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        min_height: 80

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {}
            min_width: 120

            @GUI::Label {
                text: "Family:"
                text_alignment: "CenterLeft"
                fixed_height: 16
            }

            @GUI::ListView {
                name: "family_list_view"
            }
        }

        @GUI::Widget {
            fixed_width: 100
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Label {
                text: "Style:"
                text_alignment: "CenterLeft"
                fixed_height: 16
            }

            @GUI::ListView {
                name: "variant_list_view"
            }
        }

        @GUI::Widget {
            fixed_width: 80
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Label {
                text: "Size:"
                text_alignment: "CenterLeft"
                fixed_height: 16
            }

            @GUI::SpinBox {
                name: "size_spin_box"
            }

            @GUI::ListView {
                name: "size_list_view"
            }
        }
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {}
        margins: [0]
        title: "Sample text"
        preferred_height: 80

        @GUI::Label {
            name: "sample_text_label"
            text: "The quick brown fox jumps over the lazy dog."
            preferred_height: "grow"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        preferred_height: "fit"

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
