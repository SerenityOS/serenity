@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {}
    fill_with_background_color: true

    @GUI::ToolbarContainer {
        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::HorizontalSplitter {
        layout: @GUI::HorizontalBoxLayout {}
        name: "splitter"

        @GUI::TextEditor {
            name: "text_editor"
        }

        @GUI::Frame {
            name: "preview_frame"
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
