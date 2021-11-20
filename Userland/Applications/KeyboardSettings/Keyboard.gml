@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Mapping"
        fixed_height: 200

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Character mapping file:"
                fixed_width: 130
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "character_map_file_combo"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Test your current keymap below"
                text_alignment: "CenterLeft"
            }
            @GUI::Button {
                text: "Clear"
                name: "button_clear_test_typing_area"
                fixed_width: 48
            }
        }

        @GUI::TextEditor {
            fixed_height: 100
            name: "test_typing_area"
        }
    }

    @GUI::GroupBox {
        title: "Num Lock"
        fixed_height: 60

        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 8, 8]
        }

        @GUI::CheckBox {
            text: "Enable Num Lock on login"
            name: "num_lock_checkbox"
        }
    }
}
