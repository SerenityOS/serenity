@GLClockDemo::MainWidget {
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout{
        spacing: 20
    }
        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout{

            }

            @GLClockDemo::GLClock {
                timezone: "America/New_York"
            }

           @GUI::Label {
                text: "New York"
                text_alignment: "Center"
                fixed_height: 32
            }
        }
       
        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout{

            }

            @GLClockDemo::GLClock {
                timezone: "America/Jamaica"
           }

           @GUI::Label {
              text: "Montego Bay"
              text_alignment: "Center"
              fixed_height: 32
            }
        }        
        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout{

            }

           @GLClockDemo::GLClock {
                timezone: "Europe/London"
           }
            @GUI::Label {
              text: "London"
              text_alignment: "Center"
              fixed_height: 32
            }

        }     
        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout{

            }

           @GLClockDemo::GLClock {
                timezone: "Europe/Berlin"
           }
            @GUI::Label {
              text: "Berlin"
              text_alignment: "Center"
              fixed_height: 32
            }

        } 


}
