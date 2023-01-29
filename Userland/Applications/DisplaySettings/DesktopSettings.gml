@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Layout"
        preferred_height: "shrink"
        layout: @GUI::HorizontalBoxLayout {
            margins: [8]
            spacing: 2
        }

        @GUI::Widget {
            fixed_width: 32
            layout: @GUI::VerticalBoxLayout {}

            @GUI::ImageWidget {
                bitmap: "/res/icons/32x32/workspaces.png"
            }

            @GUI::Layout::Spacer {}
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                margins: [4, 0, 0, 16]
                spacing: 10
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                    spacing: 8
                }

                @GUI::Label {
                    text: "Rows:"
                    autosize: true
                }

                @GUI::SpinBox {
                    name: "workspace_rows_spinbox"
                    min: 1
                    max: 16
                }

                @GUI::Widget {
                    fixed_width: 8
                }

                @GUI::Label {
                    text: "Columns:"
                    autosize: true
                }

                @GUI::SpinBox {
                    name: "workspace_columns_spinbox"
                    min: 1
                    max: 16
                }
            }

            @GUI::Label {
                name: "keyboard_shortcuts_label"
                text_alignment: "CenterLeft"
            }
        }
    }
}
