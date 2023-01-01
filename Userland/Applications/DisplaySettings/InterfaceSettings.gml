@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 8
    }

    @GUI::GroupBox {
        preferred_height: "shrink"
        title: "Global menu"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                    margins: [4, 0, 0, 0]
                }

                @GUI::CheckBox {
                    name: "global_menu_checkbox"
                    text: "Enable the global menu"
                }
            }
        }
    }
}
