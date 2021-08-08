@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 0
    }
    @RemoteDesktopClient::RemoteDesktopWidget {
        name: "remote_desktop"
        shadow: "Sunken"
    }
    @GUI::Statusbar {
        name: "status_bar"
        label_count: 2
    }
}
