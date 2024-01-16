@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @DisplaySettings::MonitorWidget {
        name: "monitor_widget"
        fixed_width: 304
        fixed_height: 201
    }

    @GUI::Widget {
        fixed_height: 20
    }

    @GUI::Widget {
        preferred_height: "fit"
        layout: @GUI::HorizontalBoxLayout {
            margins: [8, 8, 6, 16]
        }

        @GUI::Label {
            text: "Screen:"
            text_alignment: "CenterLeft"
            fixed_width: 55
        }

        @GUI::ComboBox {
            name: "screen_combo"
        }
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [14, 14, 4]
        }
        title: "Screen settings"

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                text: "Resolution:"
                text_alignment: "CenterLeft"
                fixed_width: 95
            }

            @GUI::ComboBox {
                name: "resolution_combo"
            }

            @GUI::Label {
                name: "display_dpi"
                text: "96 dpi"
                fixed_width: 50
            }
        }

        @GUI::Widget {
            fixed_height: 8
        }

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                text: "Display scale:"
                text_alignment: "CenterLeft"
                fixed_width: 95
            }

            @GUI::RadioButton {
                name: "scale_1x"
                text: "1x"
                fixed_width: 50
            }

            @GUI::RadioButton {
                name: "scale_2x"
                text: "2x"
                fixed_width: 50
            }

            @GUI::Layout::Spacer {}
        }
    }
}
