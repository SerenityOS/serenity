@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        spacing: 0
        margins: [4]
    }

    @GUI::TextBox {
        placeholder: "Find an emoji..."
        name: "search_textbox"
        mode: "Editable"
        preferred_width: "opportunistic_grow"
    }

    @GUI::ScrollableContainerWidget {
        should_hide_unnecessary_scrollbars: true
        name: "scrollable_emojis_widget"
        content_widget: @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                spacing: 0
            }
            name: "emojis_widget"
        }
    }
}
