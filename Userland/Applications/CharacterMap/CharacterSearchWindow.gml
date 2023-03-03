@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
        margins: [2, 0, 0, 0]
    }
    fill_with_background_color: true

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        preferred_height: "fit"

        @GUI::TextBox {
            name: "search_input"
        }

        @GUI::Button {
            name: "search_button"
            icon: "/res/icons/16x16/find.png"
            button_style: "Coolbar"
            fixed_width: 22
        }
    }

    @GUI::TableView {
        name: "results_table"
    }
}
