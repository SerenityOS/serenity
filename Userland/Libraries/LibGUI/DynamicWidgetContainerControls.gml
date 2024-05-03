@GUI::DynamicWidgetContainerControls {
    layout: @GUI::HorizontalBoxLayout {}
    preferred_height: "shrink"

    @GUI::LabelWithEventDispatcher {
        name: "section_label"
        text_alignment: "CenterLeft"
    }

    @GUI::Button {
        name: "detach_button"
        tooltip: "Detach"
        button_style: "Coolbar"
        preferred_width: "shrink"
        preferred_height: "shrink"
        icon_from_path: "/res/icons/16x16/detach.png"
    }

    @GUI::Button {
        name: "collapse_button"
        tooltip: "Collapse"
        button_style: "Coolbar"
        preferred_width: "shrink"
        preferred_height: "shrink"
        icon_from_path: "/res/icons/16x16/upward-triangle.png"
    }

    @GUI::Button {
        name: "expand_button"
        tooltip: "Expand"
        button_style: "Coolbar"
        preferred_width: "shrink"
        preferred_height: "shrink"
        icon_from_path: "/res/icons/16x16/downward-triangle.png"
    }
}
