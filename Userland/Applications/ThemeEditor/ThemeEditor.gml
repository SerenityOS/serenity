@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {}
    fill_with_background_color: true

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [0, 4, 4]
            spacing: 6
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 4
            }

            @ThemeEditor::PreviewWidget {
                name: "preview_widget"
            }

            @GUI::TabWidget {
                name: "property_tabs"
                container_margins: [5]
            }
        }

        @GUI::Widget {
            name: "theme_override_controls"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 6
            }
            preferred_height: "shrink"

            @GUI::Layout::Spacer {}

            @GUI::DialogButton {
                name: "reset_button"
                text: "Reset"
                enabled: false
            }

            @GUI::DialogButton {
                name: "apply_button"
                text: "Apply"
                enabled: false
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
