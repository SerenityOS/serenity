@GUI::Widget {
    name: "main"
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::TextBox {
        name: "search"
    }

    @GUI::HorizontalSplitter {
        @GUI::ListView {
            name: "index"
            fixed_width: 200
        }

        @GUI::TextEditor {
            name: "editor"
        }
    }
}
