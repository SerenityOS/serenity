@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::TabWidget {
        name: "tab_widget"

        @GUI::Widget {
            title: "Cookies"
            layout: @GUI::VerticalBoxLayout {
                margins: [4]
            }

            @GUI::GroupBox {
                layout: @GUI::VerticalBoxLayout {
                    margins: [6]
                }

                @GUI::TableView {
                    name: "cookies_tableview"
                }
            }
        }

        @GUI::Widget {
            title: "Local Storage"
            layout: @GUI::VerticalBoxLayout {
                margins: [4]
            }

            @GUI::GroupBox {
                layout: @GUI::VerticalBoxLayout {
                    margins: [6]
                }

                @GUI::TableView {
                    name: "local_storage_tableview"
                }
            }
        }
    }
}
