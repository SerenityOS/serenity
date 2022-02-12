@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [20]
    }

    @GUI::HorizontalSplitter {
        @GUI::Widget {
            name: "csv_options"
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::GroupBox {
                    title: "Delimiter"
                    layout: @GUI::VerticalBoxLayout {
                        margins: [10, 8, 8]
                    }

                    @GUI::RadioButton {
                        name: "delimiter_comma_radio"
                        text: "Comma"
                        autosize: true
                    }

                    @GUI::RadioButton {
                        name: "delimiter_semicolon_radio"
                        text: "Semicolon"
                        autosize: true
                    }

                    @GUI::RadioButton {
                        name: "delimiter_tab_radio"
                        text: "Tab"
                        autosize: true
                    }

                    @GUI::RadioButton {
                        name: "delimiter_space_radio"
                        text: "Space"
                        autosize: true
                    }

                    @GUI::Widget {
                        fixed_height: 25
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::RadioButton {
                            name: "delimiter_other_radio"
                            text: "Other: "
                            autosize: true
                        }

                        @GUI::TextBox {
                            name: "delimiter_other_text_box"
                            text: ""
                        }
                    }
                }

                @GUI::GroupBox {
                    title: "Quote"
                    layout: @GUI::VerticalBoxLayout {
                        margins: [10, 8, 8]
                    }

                    @GUI::RadioButton {
                        name: "quote_single_radio"
                        text: "Single Quotes"
                        autosize: true
                    }

                    @GUI::RadioButton {
                        name: "quote_double_radio"
                        text: "Double Quotes"
                        autosize: true
                    }

                    @GUI::Widget {
                        fixed_height: 25
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::RadioButton {
                            name: "quote_other_radio"
                            text: "Other: "
                            autosize: true
                        }

                        @GUI::TextBox {
                            name: "quote_other_text_box"
                            text: ""
                        }
                    }

                    @GUI::Widget {}

                    @GUI::Widget {
                        fixed_height: 25
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            text: "Escape by "
                            autosize: true
                        }

                        @GUI::ComboBox {
                            name: "quote_escape_combo_box"
                            model_only: true
                        }
                    }

                    @GUI::Widget {}
                }
            }

            @GUI::GroupBox {
                title: "Trim Field Spaces"
                fixed_height: 40
                layout: @GUI::VerticalBoxLayout {
                    margins: [4, 4, 0]
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {}

                    @GUI::CheckBox {
                        name: "trim_leading_field_spaces_check_box"
                        text: "Leading spaces"
                    }

                    @GUI::CheckBox {
                        name: "trim_trailing_field_spaces_check_box"
                        text: "Trailing spaces"
                    }
                }
            }

            @GUI::CheckBox {
                fixed_height: 15
                name: "read_header_check_box"
                text: "Read a header row"
            }
        }

        @GUI::GroupBox {
            title: "Data Preview"
            fixed_width: 150
            layout: @GUI::VerticalBoxLayout {
                margins: [10, 8, 8]
            }

            @GUI::StackWidget {
                name: "data_preview_widget"

                @GUI::TableView {
                    name: "data_preview_table_view"
                }

                @GUI::Label {
                    name: "data_preview_error_label"
                    word_wrap: true
                }
            }
        }
    }
}
