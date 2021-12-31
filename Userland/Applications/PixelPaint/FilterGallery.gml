@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
    }

    fill_with_background_color: true

    @GUI::Widget {

        layout:@GUI::HorizontalBoxLayout {
            margins: [4]
        }
    
        @GUI::TreeView {
            name: "tree_view"
        }

    }

    @GUI::Widget {
        max_height: 24

        layout:@GUI::HorizontalBoxLayout {
            margins: [4]
        }

        @GUI::Widget {}

        @GUI::Button {
            name: "apply_button"
            text: "Apply"
            max_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            max_width: 75
        }
    }
}
