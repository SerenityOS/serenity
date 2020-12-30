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
        fixed_height: 20

        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Button {
            name: "add_button"
            text: "Add"
            fixed_width: 100
            fixed_height: 20
        }

        @GUI::Button {
            name: "remove_button"
            text: "Remove"
            fixed_width: 100
            fixed_height: 20
        }
    }
}
