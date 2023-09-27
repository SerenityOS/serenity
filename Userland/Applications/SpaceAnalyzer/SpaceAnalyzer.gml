@SpaceAnalyzer::MainWidget {
    layout: @GUI::VerticalBoxLayout {
        spacing: 0
    }

    @GUI::ToolbarContainer {
        @GUI::Breadcrumbbar {
            fixed_height: 25
            name: "breadcrumbbar"
        }
    }

    @SpaceAnalyzer::TreeMapWidget {
        name: "tree_map"
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
