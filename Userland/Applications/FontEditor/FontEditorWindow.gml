@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
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
                shrink_to_fit: true

                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::SpinBox {
                    name: "glyph_editor_width_spinbox"
                }

                @GUI::CheckBox {
                    name: "glyph_editor_present_checkbox"
                    text: "Present"
                    focus_policy: "TabFocus"
                }
            }

            @GUI::ToolbarContainer {
                name: "glyph_toolbar_container"

                @GUI::Toolbar {
                    name: "glyph_mode_toolbar"
                }

                @GUI::Toolbar {
                    name: "glyph_transform_toolbar"
                }
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
                shrink_to_fit: true

                layout: @GUI::VerticalBoxLayout {
                    margins: [6, 6, 6, 6]
                }

                @GUI::Widget {
                    fixed_height: 22

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
                    fixed_height: 22

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
                    fixed_height: 22

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
                    fixed_height: 22

                    layout: @GUI::HorizontalBoxLayout {
                    }

                    @GUI::Label {
                        name: "slope_label"
                        fixed_width: 100
                        text_alignment: "CenterLeft"
                        text: "Slope:"
                    }

                    @GUI::ComboBox {
                        name: "slope_combobox"
                        model_only: true
                    }
                }

                @GUI::Widget {
                    fixed_height: 22

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
                    fixed_height: 22

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
                    fixed_height: 22

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

                    @GUI::CheckBox {
                        name: "fixed_width_checkbox"
                        text: "Fixed width"
                        autosize: true
                    }
                }
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
