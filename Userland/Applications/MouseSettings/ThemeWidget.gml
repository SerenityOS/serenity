@MouseSettings::ThemeWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Available cursor themes"
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 4
        }

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 8
            }

            @GUI::ComboBox {
                name: "theme_name_box"
            }
        }

        @GUI::TableView {
            name: "cursors_tableview"
        }
    }
}
