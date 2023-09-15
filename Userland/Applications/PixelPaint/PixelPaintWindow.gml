@GUI::Widget {
    name: "main"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 2
        }

        @GUI::ToolbarContainer {
            name: "toolbar_container"

            @PixelPaint::ToolboxWidget {
                name: "toolbox"
            }
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                spacing: 0
            }

            @GUI::TabWidget {
                name: "tab_widget"
                container_margins: [4, 5, 5, 4]
                reorder_allowed: true
                show_close_buttons: true
            }

            @PixelPaint::PaletteWidget {
                name: "palette_widget"
            }
        }

        @GUI::DynamicWidgetContainer {
            section_label: "Editor Panels"
            config_domain: "PixelPaint"
            with_individual_order: true
            fixed_width: 200
            preferred_height: "grow"
            detached_size: [200, 640]

            @GUI::DynamicWidgetContainer {
                section_label: "Layers"
                config_domain: "PixelPaint"
                preferred_height: "grow"
                detached_size: [250, 600]

                @GUI::GroupBox {
                    layout: @GUI::VerticalBoxLayout {
                        margins: [6]
                    }

                    @PixelPaint::LayerListWidget {
                        name: "layer_list_widget"
                    }
                }

                @PixelPaint::LayerPropertiesWidget {
                    name: "layer_properties_widget"
                    max_height: 94
                }
            }

            @GUI::DynamicWidgetContainer {
                section_label: "Color Visualizations"
                config_domain: "PixelPaint"

                @GUI::Widget {
                    layout: @GUI::VerticalBoxLayout {}

                    @GUI::GroupBox {
                        visible: false
                        layout: @GUI::VerticalBoxLayout {
                            margins: [6]
                        }

                        @PixelPaint::HistogramWidget {
                            name: "histogram_widget"
                            min_height: 65
                        }
                    }

                    @GUI::GroupBox {
                        min_height: 80
                        visible: false
                        layout: @GUI::VerticalBoxLayout {
                            margins: [6]
                        }

                        @PixelPaint::VectorscopeWidget {
                            name: "vectorscope_widget"
                            preferred_height: "fit"
                        }
                    }
                }
            }

            @GUI::DynamicWidgetContainer {
                section_label: "Tool Properties"
                config_domain: "PixelPaint"
                detached_size: [200, 200]

                @PixelPaint::ToolPropertiesWidget {
                    name: "tool_properties_widget"
                    max_height: 144
                }
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
