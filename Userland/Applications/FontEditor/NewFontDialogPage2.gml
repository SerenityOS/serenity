@GUI::Widget {
    layout: @GUI::HorizontalBoxLayout {
        margins: [20]
    }

    @GUI::GroupBox {
        title: "Metadata"
        fixed_width: 200
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                fixed_width: 80
                text_alignment: "CenterLeft"
                text: "Height:"
            }

            @GUI::SpinBox {
                name: "height_spinbox"
                min: 1
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                fixed_width: 80
                text_alignment: "CenterLeft"
                text: "Width:"
            }

            @GUI::SpinBox {
                name: "width_spinbox"
                min: 1
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

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
            layout: @GUI::HorizontalBoxLayout {}

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
            layout: @GUI::HorizontalBoxLayout {}

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
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Widget {
                fixed_width: 80
            }

            @GUI::CheckBox {
                name: "fixed_width_checkbox"
                text: "Fixed width"
            }
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Layout::Spacer {}

        @FontEditor::GlyphPreviewWidget {
            name: "glyph_preview_widget"
            layout: @GUI::VerticalBoxLayout {}
        }

        @GUI::Layout::Spacer {}
    }
}
