@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

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
                layout: @GUI::VerticalBoxLayout {}

                @GUI::Label {
                    name: "font_name"
                    text: "Cool Font 16 400"
                }
            }
        }
    }

    @GUI::HorizontalSplitter {
        opportunistic_resizee: "First"

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {}

            @GUI::GlyphMapWidget {
                name: "glyph_map"
            }

            @GUI::Widget {
                preferred_height: "fit"
                layout: @GUI::HorizontalBoxLayout {
                    spacing: 4
                    margins: [0, 2, 0, 2]
                }

                @GUI::TextBox {
                    name: "output_box"
                }

                @GUI::Button {
                    name: "copy_output_button"
                    icon: "/res/icons/16x16/edit-copy.png"
                    fixed_width: 22
                }
            }
        }

        @GUI::ListView {
            preferred_width: 175
            name: "unicode_block_listview"
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
