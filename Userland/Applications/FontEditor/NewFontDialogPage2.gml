@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [20]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::GroupBox {
            title: "Metadata"
            fixed_width: 200
            layout: @GUI::VerticalBoxLayout {
                margins: [16, 8, 8]
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                }

                @GUI::Label {
                    fixed_width: 80
                    text_alignment: "CenterLeft"
                    text: "Height:"
                }

                @GUI::SpinBox {
                    name: "height_spinbox"
                    min: 0
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                }

                @GUI::Label {
                    fixed_width: 80
                    text_alignment: "CenterLeft"
                    text: "Width:"
                }

                @GUI::SpinBox {
                    name: "width_spinbox"
                    min: 0
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                }

                @GUI::Label {
                    fixed_width: 80
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
                    fixed_width: 80
                    text_alignment: "CenterLeft"
                    text: "Baseline:"
                }

                @GUI::SpinBox {
                    name: "baseline_spinbox"
                    min: 0
                }
            }

            @GUI::HorizontalSeparator {
                fixed_height: 22
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                }

                @GUI::Label {
                    fixed_width: 80
                    text_alignment: "CenterLeft"
                    text: "Spacing:"
                }

                @GUI::SpinBox {
                    name: "spacing_spinbox"
                    min: 0
                    max: 255
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
                }
            }
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
            }

            @GUI::Widget {
            }

            @GUI::Widget {
                name: "glyph_editor_container"
                layout: @GUI::VerticalBoxLayout {
                    margins: [5, 0, 0]
                }
            }

            @GUI::Widget {
            }
        }
    }
}
