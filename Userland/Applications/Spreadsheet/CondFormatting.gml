@GUI::Widget {
    name: "main"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
        spacing: 4
    }

    @GUI::ScrollableContainerWidget {
        should_hide_unnecessary_scrollbars: true
        content_widget: @Spreadsheet::ConditionsView {
            name: "conditions_view"
        }
    }

    @GUI::Widget {
        preferred_height: "fit"
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "add_button"
            text: "Add"
            fixed_width: 70
        }

        @GUI::Button {
            name: "remove_button"
            text: "Remove"
            fixed_width: 70
        }

        @GUI::Layout::Spacer {}
    }
}
