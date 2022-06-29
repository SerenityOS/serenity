@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {}
    fill_with_background_color: true

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }

        @GUI::TreeView {
            name: "tree_view"
            fixed_width: 200
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                margins: [4]
            }

            @GUI::Widget {
                name: "config_widget"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }
            }

            @GUI::GroupBox {
                title: "Preview"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }

                @PixelPaint::FilterPreviewWidget {
                    name: "preview_widget"
                }
            }
        }
    }

    @GUI::Widget {
        max_height: 24
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }

        @GUI::Widget {}

        @GUI::DialogButton {
            name: "apply_button"
            text: "Apply"
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
