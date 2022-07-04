@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
        spacing: 6
    }
    fill_with_background_color: true

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::TreeView {
            name: "tree_view"
            fixed_width: 200
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Widget {
                name: "config_widget"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }
            }

            @GUI::Layout::Spacer {}

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
        layout: @GUI::HorizontalBoxLayout {
            spacing: 6
        }
        preferred_height: "fit"

        @GUI::Layout::Spacer {}

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
