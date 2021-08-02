@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10, 10, 10, 10]
    }

    @GUI::GroupBox {
        title: "Theme"

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 2, 16, 16]
        }

        @GUI::Widget {
            fixed_height: 50

            layout: @GUI::HorizontalBoxLayout {
                spacing: 10
                margins: [8, 16, 0, 8]
            }

            @GUI::Label {
                text: "Select Theme: "
                text_alignment: "CenterRight"
            }

            @GUI::ComboBox {
                name: "theme_name_box"
                model_only: true
            }
        }

        @GUI::TableView {
            name: "cursors_tableview"
            font_size: 12
        }
    }
}
