@GUI::Widget {
    name: "main"
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolBarContainer {
        @GUI::ToolBar {
            name: "toolbar"
        }
    }

    @GUI::HorizontalSplitter {
        @GUI::TextEditor {
            name: "editor"
        }

        @Web::OutOfProcessWebView {
            name: "webview"
            visible: false
        }
    }

    @GUI::Widget {
        name: "find_replace_widget"
        visible: false
        fill_with_background_color: true
        horizontal_size_policy: "Fill"
        vertical_size_policy: "Fixed"
        preferred_height: 48

        layout: @GUI::VerticalBoxLayout {
            margins: [2, 2, 2, 4]
        }

        @GUI::Widget {
            name: "find_widget"
            fill_with_background_color: true
            horizontal_size_policy: "Fill"
            vertical_size_policy: "Fixed"
            preferred_height: 22

            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Button {
                name: "find_previous_button"
                text: "Find previous"
                horizontal_size_policy: "Fixed"
                vertical_size_policy: "Fill"
                preferred_width: 150
            }

            @GUI::Button {
                name: "find_next_button"
                text: "Find next"
                horizontal_size_policy: "Fixed"
                vertical_size_policy: "Fill"
                preferred_width: 150
            }
        }

        @GUI::Widget {
            name: "replace_widget"
            fill_with_background_color: true
            horizontal_size_policy: "Fill"
            vertical_size_policy: "Fixed"
            preferred_height: 22

            layout: @GUI::HorizontalBoxLayout {
            }

            @GUI::Button {
                name: "replace_previous_button"
                text: "Replace previous"
                horizontal_size_policy: "Fixed"
                vertical_size_policy: "Fill"
                preferred_width: 100
            }

            @GUI::Button {
                name: "replace_next_button"
                text: "Replace next"
                horizontal_size_policy: "Fixed"
                vertical_size_policy: "Fill"
                preferred_width: 100
            }

            @GUI::Button {
                name: "replace_all_button"
                text: "Replace all"
                horizontal_size_policy: "Fixed"
                vertical_size_policy: "Fill"
                preferred_width: 100
            }
        }
    }

    @GUI::StatusBar {
        name: "statusbar"
    }
}

