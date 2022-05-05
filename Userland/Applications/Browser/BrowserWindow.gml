@GUI::Widget {
    name: "browser"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::HorizontalSeparator {
        name: "top_line"
        fixed_height: 2
        visible: false
    }

    @GUI::TabWidget {
        name: "tab_widget"
        container_margins: [0]
        uniform_tabs: true
        reorder_allowed: true
        show_close_buttons: true
        text_alignment: "CenterLeft"
    }
}
