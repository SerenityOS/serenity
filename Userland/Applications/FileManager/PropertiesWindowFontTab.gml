@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 12
    }

    @GUI::GroupBox {
        title: "Font"
        preferred_height: "shrink"
        layout: @GUI::VerticalBoxLayout {
            margins: [12, 8, 0]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Format:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "font_format"
                text: "TrueType"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Family:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "font_family"
                text: "SerenitySans"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Fixed width:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "font_fixed_width"
                text: "No"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Width:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "font_width"
                text: "Normal"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Weight:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "font_weight"
                text: "500 (Medium)"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Slope:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "font_slope"
                text: "Regular"
                text_alignment: "TopLeft"
            }
        }
    }
}
