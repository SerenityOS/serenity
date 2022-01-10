@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::ToolbarContainer {
        @GUI::Toolbar {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 4
                margins: [0, 0, 0, 4]
            }

            name: "toolbar"

            @GUI::Label {
                text: "Font: "
                autosize: true
            }

            @GUI::Frame {
                background_role: "Base"
                fill_with_background_color: true
                fixed_height: 20

                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Label {
                    name: "font_name"
                    text: "Cool Font 16 400"
                }
            }
        }
    }

    @GUI::GlyphMapWidget {
        name: "glyph_map"
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
