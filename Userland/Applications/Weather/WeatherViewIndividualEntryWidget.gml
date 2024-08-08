@Weather::IndividualEntryWidget {
    layout: @GUI::HorizontalBoxLayout {}
    fill_with_background_color: true
    preferred_height: "shrink"

    @GUI::DynamicWidgetContainer {
        name: "container"

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                margins: [4, 0, 0]
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Label {
                    name: "city_name"
                    font_size: 12
                    font_weight: "Bold"
                    shrink_to_fit: true
                    text_alignment: "TopLeft"
                }

                @GUI::Label {
                    name: "country_name"
                    text_alignment: "CenterLeft"
                    shrink_to_fit: true
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Widget {
                    layout: @GUI::VerticalBoxLayout {}
                    fixed_width: 120

                    @GUI::ImageWidget {
                        name: "icon"
                        fixed_width: 120
                        fixed_height: 80
                    }

                    @GUI::Label {
                        name: "description"
                        text_alignment: "CenterLeft"
                        shrink_to_fit: true
                    }
                }

                @GUI::Widget {
                    layout: @GUI::VerticalBoxLayout {
                        margins: [0]
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            text: "State:"
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "state"
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                        }
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            text: "Humidity:"
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "humidity"
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                        }

                        @GUI::Label {
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                            text: "%"
                        }
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            text: "Temperature:"
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "temp_current"
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                        }

                        @GUI::Label {
                            name: "temp_min_max"
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                        }

                        @GUI::Label {
                            name: "temp_unit"
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                        }
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            text: "Feels like:"
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "feels_like"
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                        }

                        @GUI::Label {
                            name: "feels_like_unit"
                            shrink_to_fit: true
                            text_alignment: "CenterLeft"
                        }
                    }

                    @GUI::Widget {}

                    @GUI::Widget {}
                }

                @GUI::Frame {
                    fixed_width: 1
                    frame_style: "RaisedBox"
                }

                @GUI::Widget {
                    layout: @GUI::VerticalBoxLayout {
                        margins: [0]
                        spacing: 0
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "forecast1"
                            text_alignment: "CenterLeft"
                            shrink_to_fit: true
                        }
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "forecast2"
                            text_alignment: "CenterLeft"
                            shrink_to_fit: true
                        }
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "forecast3"
                            text_alignment: "CenterLeft"
                            shrink_to_fit: true
                        }
                    }

                    @GUI::Widget {
                        layout: @GUI::HorizontalBoxLayout {}

                        @GUI::Label {
                            shrink_to_fit: true
                        }

                        @GUI::Label {
                            name: "forecast4"
                            text_alignment: "CenterLeft"
                            shrink_to_fit: true
                        }
                    }

                    @GUI::Widget {}
                }
            }
        }
    }
}
