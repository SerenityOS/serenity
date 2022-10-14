@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::HorizontalSplitter {
        @GUI::TreeView {
            name: "mailbox_list"
            preferred_width: 250
        }

        @GUI::VerticalSplitter {
            @GUI::TableView {
                name: "individual_mailbox_view"
            }

            @WebView::OutOfProcessWebView {
                name: "web_view"
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
