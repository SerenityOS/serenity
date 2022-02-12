@GUI::Widget {
    name: "main"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
        spacing: 4
    }

    @Spreadsheet::ConditionsView {
        name: "conditions_view"
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Widget {}

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

        @GUI::Widget {}
    }
}
