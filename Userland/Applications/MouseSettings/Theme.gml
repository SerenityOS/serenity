@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
    }

    @GUI::GroupBox {
        title: "Available Cursor Themes"

        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 4
        }

        @GUI::Widget {
            shrink_to_fit: true

            layout: @GUI::HorizontalBoxLayout {
                spacing: 8
            }

            @GUI::ComboBox {
                name: "theme_name_box"
                model_only: true
            }
        }

        @GUI::TableView {
            name: "cursors_tableview"
        }
    }
}
