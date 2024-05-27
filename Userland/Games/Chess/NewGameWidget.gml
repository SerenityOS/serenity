@Chess::NewGameWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Button {
            name: "start_button"
            text: "Start"
            fixed_width: 80
        }
    }
}
