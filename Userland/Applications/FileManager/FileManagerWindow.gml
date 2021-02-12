@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolBarContainer {
        name: "toolbar_container"
        @GUI::ToolBar {
            name: "main_toolbar"
        }
        @GUI::ToolBar {
            name: "location_toolbar"
            visible: false

            @GUI::Label {
                text: "Location: "
                autosize: true
            }

            @GUI::TextBox {
                name: "location_textbox"
                fixed_height: 22
            }
        }
        @GUI::ToolBar {
            name: "breadcrumb_toolbar"

            @GUI::Label {
                text: "Location: "
                autosize: true
            }

            @GUI::BreadcrumbBar {
                name: "breadcrumb_bar"
            }
        }
    }

    @GUI::HorizontalSplitter {
        name: "splitter"

        @GUI::TreeView {
            name: "tree_view"
            fixed_width: 175
        }

    }

    @GUI::StatusBar {
        name: "statusbar"

        @GUI::ProgressBar {
            name: "progressbar"
            text: "Generating thumbnails: "
            visible: false
        }
    }
}
