@KeyboardSettings::KeyboardSettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Mapping"
        fixed_height: 150
        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::Widget {
            fixed_width: 32
            layout: @GUI::VerticalBoxLayout {}

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/icons/32x32/app-keyboard-mapper.png"
            }
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                spacing: 2
            }

            @GUI::ListView {
                name: "selected_keymaps"
            }

            @GUI::Widget {
                fixed_height: 24
                layout: @GUI::HorizontalBoxLayout {
                    spacing: 4
                }

                @GUI::Button {
                    name: "activate_keymap_button"
                    text: "Activate Keymap"
                    enabled: false
                }

                @GUI::Button {
                    name: "add_keymap_button"
                    text: "Add Keymap..."
                }

                @GUI::Button {
                    name: "remove_keymap_button"
                    text: "Remove Keymap"
                    enabled: false
                }
            }
        }
    }

    @GUI::GroupBox {
        title: "Test input"
        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::Widget {
            fixed_width: 32
            layout: @GUI::VerticalBoxLayout {}

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/icons/32x32/app-keyboard-settings.png"
            }
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                spacing: 2
            }

            @GUI::Widget {
                fixed_height: 24
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
                name: "test_typing_area"
            }
        }
    }

    @GUI::GroupBox {
        title: "Num Lock"
        fixed_height: 60
        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::ImageWidget {
            fixed_width: 32
            fixed_height: 32
            bitmap: "/res/icons/32x32/app-calculator.png"
        }

        @GUI::CheckBox {
            text: "Enable Num Lock on login"
            name: "num_lock_checkbox"
        }
    }

    @GUI::GroupBox {
        title: "Caps Lock"
        fixed_height: 60
        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::CheckBox {
            text: "Use Caps Lock as an additional Ctrl"
            name: "caps_lock_remapped_to_ctrl_checkbox"
        }
    }
}
