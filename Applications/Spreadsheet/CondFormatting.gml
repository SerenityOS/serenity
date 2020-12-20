@GUI::Widget {
    name: "main"
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 4
    }

    @Spreadsheet::ConditionsView {
        name: "conditions_view"
    }

    @GUI::Widget {
        vertical_size_policy: "Fixed"
        horizontal_size_policy: "Fill"
        preferred_width: 0
        preferred_height: 20

        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Button {
            name: "add_button"
            text: "Add"
            horizontal_size_policy: "Fixed"
            vertical_size_policy: "Fixed"
            preferred_width: 100
            preferred_height: 20
        }

        @GUI::Button {
            name: "remove_button"
            text: "Remove"
            horizontal_size_policy: "Fixed"
            vertical_size_policy: "Fixed"
            preferred_width: 100
            preferred_height: 20
        }
    }
}
