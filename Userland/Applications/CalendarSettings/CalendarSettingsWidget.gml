@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Preferred first day of week"
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
}
