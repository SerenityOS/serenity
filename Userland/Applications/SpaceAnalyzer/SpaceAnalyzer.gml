@GUI::Widget {
	layout: @GUI::VerticalBoxLayout {
		spacing: 0
	}
	
	@GUI::ToolBarContainer {
		@GUI::BreadcrumbBar {
			fixed_height: 25
			name: "breadcrumb_bar"
		}
	}
	
	@SpaceAnalyzer::TreeMapWidget {
		name: "tree_map"
	}
	
	@GUI::StatusBar {
		name: "status_bar"
	}
}
