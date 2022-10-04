@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Preferred First Day of Week"
        fixed_height: 72
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Label {
            text: "Determines which day a week starts with in the calendar view."
            word_wrap: true
            text_alignment: "CenterLeft"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "First day:"
                text_alignment: "CenterLeft"
                fixed_width: 70
            }

            @GUI::ComboBox {
                name: "first_day_of_week"
            }
        }
    }

    @GUI::GroupBox {
        title: "Preferred Weekend Configuration"
        fixed_height: 72
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Label {
            text: "Determines the start and length of the weekend."
            word_wrap: true
            text_alignment: "CenterLeft"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {}

                @GUI::Label {
                    text: "First day:"
                    text_alignment: "CenterLeft"
                    fixed_width: 84
                }

                @GUI::ComboBox {
                    name: "first_day_of_weekend"
                }
            }

            @GUI::Widget {
                layout: @GUI::HorizontalBoxLayout {
                    spacing: 8
                }

                @GUI::Label {
                    text: "Length:"
                    text_alignment: "CenterLeft"
                }

                @GUI::SpinBox {
                    name: "weekend_length"
                    min: 0
                    max: 7
                }

                @GUI::Label {
                    text: "Days"
                    text_alignment: "CenterLeft"
                }
            }
        }
    }

    @GUI::GroupBox {
        title: "Default View"
        fixed_height: 72
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
            spacing: 2
        }

        @GUI::Label {
            text: "Show the month or the year view when Calendar is started."
            word_wrap: true
            text_alignment: "CenterLeft"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Default view:"
                text_alignment: "CenterLeft"
                fixed_width: 70
            }

            @GUI::ComboBox {
                name: "default_view"
            }
        }
    }
}
