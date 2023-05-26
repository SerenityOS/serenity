@BrowserSettings::BrowserSettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Homepage"
        fixed_height: 70
        layout: @GUI::VerticalBoxLayout {
            margins: [2, 8, 2]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/icons/32x32/home.png"
            }

            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {}

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                        spacing: 16
                    }

                    @GUI::Label {
                        text: "URL:"
                        text_alignment: "CenterLeft"
                        fixed_width: 45
                    }

                    @GUI::TextBox {
                        name: "homepage_url_textbox"
                        placeholder: "https://example.com"
                    }
                }

                @GUI::Widget {
                    layout: @GUI::HorizontalBoxLayout {
                        spacing: 16
                    }

                    @GUI::Label {
                        text: "New tab:"
                        text_alignment: "CenterLeft"
                        fixed_width: 45
                    }

                    @GUI::TextBox {
                        name: "new_tab_url_textbox"
                        placeholder: "https://example.com"
                    }
                }
            }
        }
    }

    @GUI::GroupBox {
        title: "Appearance"
        fixed_height: 104
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/icons/32x32/color-chooser.png"
            }

            @GUI::Label {
                text: "Color scheme:"
                text_alignment: "CenterLeft"
                fixed_width: 110
            }

            @GUI::ComboBox {
                name: "color_scheme_combobox"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                fixed_width: 32
            }

            @GUI::CheckBox {
                name: "show_bookmarks_bar_checkbox"
                text: "Show bookmarks"
            }
        }
    }

    @GUI::GroupBox {
        title: "Search Engine"
        fixed_height: 140
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/icons/32x32/search-engine.png"
            }

            @GUI::CheckBox {
                text: "Search in the URL box"
                name: "enable_search_engine_checkbox"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }
            name: "search_engine_combobox_group"

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                text: "Search engine:"
                text_alignment: "CenterLeft"
                fixed_width: 110
            }

            @GUI::ComboBox {
                name: "search_engine_combobox"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }
            name: "custom_search_engine_group"

            @GUI::Widget {
                fixed_width: 32
            }

            @GUI::Label {
                text: "Enter URL template:"
                text_alignment: "CenterLeft"
                fixed_width: 110
            }

            @GUI::TextBox {
                name: "custom_search_engine_textbox"
                placeholder: "https://host/search?q={}"
            }
        }
    }

    @GUI::GroupBox {
        title: "Downloads"
        fixed_height: 70
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::ImageWidget {
                fixed_width: 32
                fixed_height: 32
                bitmap: "/res/icons/32x32/downloads.png"
            }

            @GUI::CheckBox {
                name: "auto_close_download_windows_checkbox"
                text: "Automatically close download window when complete"
            }
        }
    }
}
