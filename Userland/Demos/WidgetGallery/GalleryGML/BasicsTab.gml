@GUI::Widget {
    name: "basics_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        fixed_height: 95
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::HorizontalSplitter {
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Frame {
                name: "label_frame"
                shape: "Panel"
                shadow: "Sunken"
                thickness: 1
                layout: @GUI::VerticalBoxLayout {
                    margins: [3, 4]
                }

                @GUI::Label {
                    name: "enabled_label"
                    text: "Label"
                }

                @GUI::Label {
                    name: "disabled_label"
                    text: "Disabled"
                    enabled: false
                }
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                    margins: [0, 4]
                }

                @GUI::Label {
                    name: "word_wrap_label"
                    word_wrap: true
                    text_alignment: "TopLeft"
                    text: "Lorem ipsum sistema serenitas, per construxit klingre sed quis awesomatia, ergo salve amici."
                }
            }
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::Widget {
            fixed_height: 22
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::ComboBox {
                name: "frame_shape_combobox"
                placeholder: "Combo box"
            }

            @GUI::ComboBox {
                placeholder: "Disabled"
                enabled: false
            }

            @GUI::VerticalSeparator {
            }

            @GUI::SpinBox {
                name: "thickness_spinbox"
                min: 0
                max: 2
            }

            @GUI::SpinBox {
                enabled: false
            }
        }
    }

    @GUI::Widget {
        fixed_height: 125
        layout: @GUI::VerticalBoxLayout {
            margins: [3, 8]
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Widget {
                }

                @GUI::Button {
                    name: "normal_button"
                    text: "Button"
                }

                @GUI::Button {
                    name: "disabled_normal_button"
                    text: "Disabled"
                    enabled: "false"
                }

                @GUI::Widget {
                }
            }

            @GUI::VerticalSeparator {
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Widget {
                }

                @GUI::Button {
                    name: "enabled_coolbar_button"
                    text: "Coolbar button"
                    button_style: "Coolbar"
                }

                @GUI::Button {
                    name: "disabled_coolbar_button"
                    text: "Disabled"
                    enabled: "false"
                    button_style: "Coolbar"
                }

                @GUI::Widget {
                }
            }
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                }

                @GUI::Widget {
                    fixed_width: 60
                    layout: @GUI::VerticalBoxLayout {
                    }

                    @GUI::Widget {
                    }

                    @GUI::RadioButton {
                        name: "top_radiobutton"
                        text: "Radio 1"
                        checked: true
                    }

                    @GUI::RadioButton {
                        name: "bottom_radiobutton"
                        text: "Radio 2"
                    }

                    @GUI::Widget {
                    }
                }

                @GUI::Widget {
                }

                @GUI::Widget {
                    fixed_width: 70
                    layout: @GUI::VerticalBoxLayout {
                    }

                    @GUI::Widget {
                    }

                    @GUI::CheckBox {
                        name: "top_checkbox"
                        text: "Checkbox"
                    }

                    @GUI::CheckBox {
                        name: "bottom_checkbox"
                        text: "Disabled"
                        enabled: false
                    }

                    @GUI::Widget {
                    }
                }

                @GUI::Widget {
                }
            }

            @GUI::VerticalSeparator {
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Widget {
                }

                @GUI::Button {
                    name: "icon_button"
                    text: "Icon button"

                }

                @GUI::Button {
                    name: "disabled_icon_button"
                    text: "Disabled"
                    enabled: "false"

                }

                @GUI::Widget {
                }
            }
        }
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::Widget {
            fixed_height: 47
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::TextBox {
                    placeholder: "Text box"
                    mode: "Editable"
                }

                @GUI::TextBox {
                    text: "Disabled"
                    enabled: false
                }
            }

            @GUI::VerticalSeparator {
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::TextBox {
                    text: "Read only"
                    mode: "ReadOnly"
                }

                @GUI::TextBox {
                    text: "Display only"
                    mode: "DisplayOnly"
                }
            }
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::TextEditor {
                name: "text_editor"
                placeholder: "Text editor"
            }

            @GUI::VerticalSeparator {
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Widget {
                    fixed_height: 22
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::ColorInput {
                        name: "font_colorinput"
                        placeholder: "Color dialog"
                    }

                    @GUI::ColorInput {
                        placeholder: "Disabled"
                        enabled: false
                    }
                }

                @GUI::Widget {
                }

                @GUI::Button {
                    name: "font_button"
                    text: "Font picker dialog..."
                }

                @GUI::Button {
                    name: "file_button"
                    text: "File picker dialog..."
                }

                @GUI::Button {
                    name: "input_button"
                    text: "Input dialog..."
                }

                @GUI::Widget {
                }
            }
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::Widget {
            fixed_height: 22
            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                }

                @GUI::ComboBox {
                    name: "msgbox_icon_combobox"
                    model_only: true
                }

                @GUI::ComboBox {
                    name: "msgbox_buttons_combobox"
                    model_only: true
                }
            }

            @GUI::VerticalSeparator {
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                }

                @GUI::Button {
                    name: "msgbox_button"
                    text: "Message box dialog..."
                }
            }
        }
    }
}
