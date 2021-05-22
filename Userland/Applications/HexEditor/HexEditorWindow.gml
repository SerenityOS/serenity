@GUI::Widget {
    name: "main"
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @HexEditor::HexEditor {
        name: "editor"
    }

    @GUI::Statusbar {
        name: "statusbar"
        label_count: 5
    }
}
