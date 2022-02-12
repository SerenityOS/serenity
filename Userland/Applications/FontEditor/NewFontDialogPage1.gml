@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [20]
    }

    @GUI::Widget {
        fixed_height: 160
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
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
                fixed_width: 100
                text_alignment: "CenterLeft"
                text: "Family:"
            }

            @GUI::TextBox {
                name: "family_textbox"
            }
        }

        @GUI::HorizontalSeparator {
            fixed_height: 22
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                text: "Weight:"
                fixed_width: 100
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "weight_combobox"
                fixed_width: 180
                model_only: true
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                fixed_width: 100
                text_alignment: "CenterLeft"
                text: "Slope:"
            }

            @GUI::ComboBox {
                name: "slope_combobox"
                fixed_width: 180
                model_only: true
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                fixed_width: 100
                text_alignment: "CenterLeft"
                text: "Presentation size:"
            }

            @GUI::SpinBox {
                name: "presentation_spinbox"
                min: 0
                max: 255
                fixed_width: 180
            }
        }
    }
}
