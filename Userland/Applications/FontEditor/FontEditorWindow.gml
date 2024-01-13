@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
            collapsible: true
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Widget {
            name: "left_column_container"
            preferred_width: "shrink"
            layout: @GUI::VerticalBoxLayout {}

            @FontEditor::GlyphEditorWidget {
                name: "glyph_editor_widget"
                visible: false
            }

            @GUI::Widget {
                name: "width_control_container"
                preferred_height: "shrink"
                visible: false
                layout: @GUI::VerticalBoxLayout {}

                @GUI::SpinBox {
                    name: "glyph_editor_width_spinbox"
                    preferred_width: "fit"
                    min: 0
                }

                @GUI::CheckBox {
                    name: "glyph_editor_present_checkbox"
                    preferred_width: "fit"
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

            @GUI::HorizontalSplitter {
                opportunistic_resizee: "First"

                @GUI::Widget {
                    layout: @GUI::VerticalBoxLayout {}

                    @GUI::GlyphMapWidget {
                        name: "glyph_map_widget"
                    }

                    @GUI::GroupBox {
                        name: "font_metadata_groupbox"
                        title: "Metadata"
                        preferred_height: "shrink"
                        layout: @GUI::VerticalBoxLayout {
                            margins: [6, 6, 6, 6]
                        }

                        @GUI::Widget {
                            layout: @GUI::HorizontalBoxLayout {}

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
                            layout: @GUI::HorizontalBoxLayout {}

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
                            layout: @GUI::HorizontalBoxLayout {}

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
                            layout: @GUI::HorizontalBoxLayout {}

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
                            layout: @GUI::HorizontalBoxLayout {}

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
                            layout: @GUI::HorizontalBoxLayout {}

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
                            layout: @GUI::HorizontalBoxLayout {}

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
                            layout: @GUI::HorizontalBoxLayout {}

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

                @GUI::Widget {
                    name: "unicode_block_container"
                    preferred_width: 175
                    layout: @GUI::VerticalBoxLayout {}

                    @GUI::TextBox {
                        name: "search_textbox"
                        placeholder: "Search"
                    }

                    @GUI::ListView {
                        name: "unicode_block_listview"
                    }
                }
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 2
    }
}
