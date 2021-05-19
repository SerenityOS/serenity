@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::Widget {
            name: "left_column_container"
            layout: @GUI::VerticalBoxLayout {
            }

            @GUI::Widget {
                name: "glyph_editor_container"
                layout: @GUI::VerticalBoxLayout {
                }
            }

            @GUI::Widget {
                fixed_height: 22
                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::SpinBox {
                        name: "glyph_editor_width_spinbox"
                    }

                    @GUI::CheckBox {
                        name: "glyph_editor_present_checkbox"
                        text: "Show"
                    }

                    @GUI::Button {
                        name: "move_glyph_button"
                        fixed_width: 22
                        tooltip: "Move Glyph"
                        button_style: "Coolbar"
                    }
                }
            }

            @GUI::Widget {
            }
        }

        @GUI::Widget {
            name: "right_column_container"
            layout: @GUI::VerticalBoxLayout {
                spacing: 6
            }

            @GUI::Widget {
                name: "glyph_map_container"
                layout: @GUI::VerticalBoxLayout {
                }
            }

            @GUI::GroupBox {
                name: "font_metadata_groupbox"
                title: "Metadata"
                fixed_height: 220
                layout: @GUI::VerticalBoxLayout {
                    margins: [8, 16, 8, 4]
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "name_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Name:"
                    }

                    @GUI::TextBox {
                        name: "name_textbox"
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "family_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Family:"
                    }

                    @GUI::TextBox {
                        name: "family_textbox"
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "weight_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Weight:"
                    }

                    @GUI::ComboBox {
                        name: "weight_combobox"
                        model_only: true
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "presentation_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Presentation size:"
                    }

                    @GUI::SpinBox {
                        name: "presentation_spinbox"
                        min: 0
                        max: 255
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "spacing_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Glyph spacing:"
                    }

                    @GUI::SpinBox {
                        name: "spacing_spinbox"
                        min: 0
                        max: 255
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "mean_line_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Mean line:"
                    }

                    @GUI::SpinBox {
                        name: "mean_line_spinbox"
                        min: 0
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "baseline_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Baseline:"
                    }

                    @GUI::SpinBox {
                        name: "baseline_spinbox"
                        min: 0
                    }
                }

                @GUI::Widget {
                    fixed_height: 22
                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::CheckBox {
                        name: "fixed_width_checkbox"
                        text: "Fixed width"
                        autosize: true
                    }

                    @GUI::Widget {
                        fixed_width: 16
                    }

                    @GUI::ComboBox {
                        name: "type_combobox"
                        model_only: true
                    }
                }
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
