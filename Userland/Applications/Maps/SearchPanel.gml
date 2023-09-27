@Maps::SearchPanel {
    min_width: 100
    preferred_width: 200
    max_width: 350
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::Frame {
        frame_style: "SunkenPanel"
        fixed_height: 28
        layout: @GUI::HorizontalBoxLayout {
            margins: [2]
            spacing: 2
        }

        @GUI::TextBox {
            name: "search_textbox"
            placeholder: "Search a place..."
        }

        @GUI::Button {
            name: "search_button"
            icon_from_path: "/res/icons/16x16/find.png"
            fixed_width: 24
        }
    }

    // Start, empty and places are toggled in visibility
    @GUI::Frame {
        name: "start_container"
        frame_style: "SunkenPanel"
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }

        @GUI::Label {
            text: "Enter a search query to search for places..."
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Frame {
        name: "empty_container"
        frame_style: "SunkenPanel"
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }

        @GUI::Label {
            text: "Can't find any places with the search query"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::ListView {
        name: "places_list"
        horizontal_padding: 6
        vertical_padding: 4
        should_hide_unnecessary_scrollbars: true
        alternating_row_colors: false
    }
}
