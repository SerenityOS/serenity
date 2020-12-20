@GUI::Widget {
    name: "browser"
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::TabWidget {
        name: "tab_widget"
        container_padding: 0
        uniform_tabs: true
        text_alignment: "CenterLeft"
    }
}
