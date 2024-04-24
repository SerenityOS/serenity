@GUI::EmojiInputDialogWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::TextBox {
        name: "search_box"
        placeholder: "Search emoji"
        fixed_height: 22
    }

    @GUI::ToolbarContainer {
        @GUI::Toolbar {
            name: "toolbar"

            @GUI::Label {
                text: "Category: "
                autosize: true
            }
        }
    }

    @GUI::ScrollableContainerWidget {
        name: "scrollable_container"
        content_widget: @GUI::Widget {
            name: "emojis"
            layout: @GUI::VerticalBoxLayout {}
        }
    }
}
