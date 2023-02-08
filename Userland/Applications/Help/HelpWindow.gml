@Help::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::HorizontalSplitter {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::TabWidget {
            name: "tab_widget"
            preferred_width: 200
            container_margins: [6]

            @GUI::TreeView {
                name: "browse_view"
                title: "Browse"
            }

            @GUI::Widget {
                name: "search_container"
                title: "Search"
                layout: @GUI::VerticalBoxLayout {}

                @GUI::TextBox {
                    name: "search_box"
                    placeholder: "Search"
                }

                @GUI::ListView {
                    name: "search_view"
                }
            }
        }

        @WebView::OutOfProcessWebView {
            name: "web_view"
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
