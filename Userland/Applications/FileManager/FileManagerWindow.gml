@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "main_toolbar"
        }

        @GUI::Toolbar {
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

        @GUI::Toolbar {
            name: "breadcrumb_toolbar"

            @GUI::Label {
                text: "Location: "
                autosize: true
            }

            @GUI::Breadcrumbbar {
                name: "breadcrumbbar"
            }
        }
    }

    @GUI::HorizontalSplitter {
        name: "splitter"
        first_resizee_minimum_size: 80

        @GUI::TreeView {
            name: "tree_view"
            fixed_width: 175
        }
    }

    @GUI::Statusbar {
        name: "statusbar"

        @GUI::Progressbar {
            name: "progressbar"
            text: "Generating thumbnails: "
            visible: false
        }
    }
}
