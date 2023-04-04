@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 8
    }

    @GUI::GroupBox {
        preferred_height: "shrink"
        title: "Animations"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
                layout: @GUI::VerticalBoxLayout {}

                @GUI::ImageWidget {
                    bitmap: "/res/icons/32x32/animation-effects.png"
                }

                @GUI::Layout::Spacer {}
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                    margins: [4, 0, 0, 0]
                }

                @GUI::CheckBox {
                    name: "animate_menus_checkbox"
                    text: "Fade menus on activation"
                }

                @GUI::CheckBox {
                    name: "flash_menus_checkbox"
                    text: "Flash menus activated by keyboard shortcuts"
                }

                @GUI::CheckBox {
                    name: "animate_windows_checkbox"
                    text: "Animate windows when minimizing and maximizing"
                }

                @GUI::CheckBox {
                    name: "smooth_scrolling_checkbox"
                    text: "Use smooth scrolling"
                }
            }
        }
    }

    @GUI::GroupBox {
        preferred_height: "shrink"
        title: "Appearance"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                fixed_width: 32
                layout: @GUI::VerticalBoxLayout {}

                @GUI::ImageWidget {
                    bitmap: "/res/icons/32x32/theming-effects.png"
                }

                @GUI::Layout::Spacer {}
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                    margins: [4, 0, 0, 0]
                }

                @GUI::CheckBox {
                    name: "tab_accents_checkbox"
                    text: "Show accents on tabs"
                }

                @GUI::CheckBox {
                    name: "splitter_knurls_checkbox"
                    text: "Show knurls on splitters"
                }

                @GUI::CheckBox {
                    name: "tooltips_checkbox"
                    text: "Show tooltips"
                }

                @GUI::Widget {
                    fixed_height: 4
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {}

                    @GUI::Label {
                        text: "Show window geometry:"
                        autosize: true
                    }

                    @GUI::Layout::Spacer {}

                    @GUI::ComboBox {
                        name: "geometry_combobox"
                        fixed_width: 130
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {}

                    @GUI::Label {
                        text: "Tile Window Behavior:"
                        autosize: true
                    }

                    @GUI::Layout::Spacer {}

                    @GUI::ComboBox {
                        name: "tile_window_combobox"
                        fixed_width: 130
                    }
                }

                @GUI::Widget {
                    fixed_height: 4
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {}

                    @GUI::Label {
                        text: "Shadows"
                        autosize: true
                    }

                    @GUI::HorizontalSeparator {}
                }

                @GUI::CheckBox {
                    name: "menu_shadow_checkbox"
                    text: "Show menu shadow"
                }

                @GUI::CheckBox {
                    name: "window_shadow_checkbox"
                    text: "Show window shadow"
                }

                @GUI::CheckBox {
                    name: "tooltip_shadow_checkbox"
                    text: "Show tooltip shadow"
                }
            }
        }
    }
}
