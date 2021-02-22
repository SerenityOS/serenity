@GUI::Widget {
    name: "main"
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolBarContainer {
        name: "toolbar_container"

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

    @GUI::GroupBox {
        name: "find_replace_widget"
        visible: false
        fill_with_background_color: true
        fixed_height: 56

        layout: @GUI::VerticalBoxLayout {
            spacing: 2
            margins: [5, 5, 5, 5]
        }

        @GUI::Widget {
            name: "find_widget"
            fill_with_background_color: true
            fixed_height: 22

            layout: @GUI::HorizontalBoxLayout {
                spacing: 4
            }

            @GUI::Button {
                name: "find_previous_button"
                fixed_width: 38
            }

            @GUI::Button {
                name: "find_next_button"
                fixed_width: 38
            }

            @GUI::TextBox {
                name: "find_textbox"
            }

            @GUI::CheckBox {
                name: "regex_checkbox"
                text: "Use RegEx"
                fixed_width: 80
            }

            @GUI::CheckBox {
                name: "match_case_checkbox"
                text: "Match case"
                fixed_width: 85
            }
        }

        @GUI::Widget {
            name: "replace_widget"
            fill_with_background_color: true
            fixed_height: 22

            layout: @GUI::HorizontalBoxLayout {
                spacing: 4
            }

            @GUI::Button {
                name: "replace_button"
                text: "Replace"
                fixed_width: 80
            }

            @GUI::TextBox {
                name: "replace_textbox"
            }

            @GUI::Button {
                name: "replace_all_button"
                text: "Replace all"
                fixed_width: 80
            }

            @GUI::CheckBox {
                name: "wrap_around_checkbox"
                text: "Wrap around"
                fixed_width: 85
            }
        }
    }

    @GUI::StatusBar {
        name: "statusbar"
    }
}
