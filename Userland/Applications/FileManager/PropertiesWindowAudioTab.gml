@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 12
    }

    @GUI::GroupBox {
        title: "Audio"
        preferred_height: "shrink"
        layout: @GUI::VerticalBoxLayout {
            margins: [12, 8, 0]
            spacing: 2
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Type:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_type"
                text: "MP3"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Duration:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_duration"
                text: "3:21"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Sample rate:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_sample_rate"
                text: "44100 Hz"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Format:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_format"
                text: "16-bit"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Channels:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_channels"
                text: "2 (Stereo)"
                text_alignment: "TopLeft"
            }
        }
    }

    @GUI::GroupBox {
        title: "Track"
        layout: @GUI::VerticalBoxLayout {
            margins: [12, 8, 0]
            spacing: 2
        }

        @GUI::Widget {
            preferred_height: "shrink"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Title:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_title"
                text: "Ana Ng"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            preferred_height: "shrink"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Artist(s):"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_artists"
                text: "They Might Be Giants"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            preferred_height: "shrink"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Album:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_album"
                text: "Lincoln"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            preferred_height: "shrink"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Track number:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_track_number"
                text: "1"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            preferred_height: "shrink"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Genre:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                name: "audio_genre"
                text: "Alternative"
                text_alignment: "TopLeft"
            }
        }

        @GUI::Widget {
            preferred_height: "grow"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 12
            }

            @GUI::Label {
                text: "Comment:"
                text_alignment: "TopLeft"
                fixed_width: 80
            }

            @GUI::Label {
                preferred_height: "grow"
                name: "audio_comment"
                text: "Ana Ng and I are getting old and we still haven't walked in the glow of each other's majestic presence."
                text_alignment: "TopLeft"
            }
        }
    }
}
