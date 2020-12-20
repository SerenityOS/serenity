@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolBarContainer {
        @GUI::ToolBar {
            name: "main_toolbar"
        }
        @GUI::ToolBar {
            name: "location_toolbar"
            visible: false

            @GUI::Label {
                name: "location_label"
                text: "Location: "
            }

            @GUI::TextBox {
                name: "location_textbox"
                vertical_size_policy: "Fixed"
                preferred_height: 22
            }
        }
        @GUI::ToolBar {
            name: "breadcrumb_toolbar"

            @GUI::BreadcrumbBar {
                name: "breadcrumb_bar"
            }
        }
    }

    @GUI::HorizontalSplitter {
        name: "splitter"

        @GUI::TreeView {
            name: "tree_view"
            horizontal_size_policy: "Fixed"
            preferred_width: 175
        }

    }

    @GUI::StatusBar {
        name: "statusbar"

        @GUI::ProgressBar {
            name: "progressbar"
            visible: false
        }
    }
}
