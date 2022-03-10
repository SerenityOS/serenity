@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8, 8, 8, 8]
        auto_resize: true
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 16
        }

        @GUI::ImageWidget {
            name: "icon"
        }

        @GUI::Label {
            text: "Some application(s) prevent you from exiting.\nIf you choose \"Ignore\", you may lose unsaved changes."
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 25

        @GUI::Widget {}

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Button {
                name: "cancel"
                text: "Cancel"
            }

            @GUI::Button {
                name: "ignore"
                text: "Ignore"
            }
        }
    }
}
