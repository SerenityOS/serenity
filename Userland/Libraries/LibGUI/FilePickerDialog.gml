@GUI::FilePickerDialogWidget {
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout {
        margins: [4]
        spacing: 3
    }

    @GUI::Widget {
        preferred_width: 103
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
            min_width: 60
        }

        @GUI::Label {
            text: "Filename:"
            text_alignment: "CenterRight"
            fixed_height: 22
        }

        @GUI::Label {
            name: "allowed_file_types_label"
            text: "Files of Type:"
            text_alignment: "CenterRight"
            fixed_height: 22
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::TextBox {
                name: "location_textbox"
                preferred_width: "opportunistic_grow"
                min_width: 80
            }

            @GUI::Toolbar {
                name: "toolbar"
                preferred_width: "shrink"
            }
        }

        @GUI::MultiView {
            name: "view"
        }

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Widget {
                fixed_height: 22
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::TextBox {
                    name: "filename_textbox"
                }

                @GUI::DialogButton {
                    name: "ok_button"
                    text: "OK"
                }
            }

            @GUI::Widget {
                fixed_height: 22
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::ComboBox {
                    name: "allowed_file_type_filters_combo"
                }

                @GUI::DialogButton {
                    name: "cancel_button"
                    text: "Cancel"
                }
            }
        }
    }
}
