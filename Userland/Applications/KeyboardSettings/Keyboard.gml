@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Mapping"
        fixed_height: 60

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
