/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/ClientConnection.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibVideo/MatroskaReader.h>
#include <LibVideo/VP9/Decoder.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto window = GUI::Window::construct();

    auto document = Video::MatroskaReader::parse_matroska_from_file("/home/anon/Videos/test-webm.webm");
    auto const& optional_track = document->track_for_track_type(Video::TrackEntry::TrackType::Video);
    if (!optional_track.has_value())
        return 1;
    auto const& track = optional_track.value();
    auto const video_track = track.video_track().value();

    auto image = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, Gfx::IntSize(video_track.pixel_height, video_track.pixel_width)).release_value_but_fixme_should_propagate_errors();
    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    auto& image_widget = main_widget.add<GUI::ImageWidget>();
    image_widget.set_bitmap(image);
    image_widget.set_fixed_size(video_track.pixel_height, video_track.pixel_width);
    main_widget.add_child(image_widget);

    Video::VP9::Decoder vp9_decoder;
    for (auto const& cluster : document->clusters()) {
        for (auto const& block : cluster.blocks()) {
            if (block.track_number() != track.track_number())
                continue;

            auto const& frame = block.frame(0);
            dbgln("Reading frame 0 from block @ {}", block.timestamp());
            bool failed = !vp9_decoder.decode_frame(frame);
            vp9_decoder.dump_frame_info();
            if (failed)
                return 1;
        }
    }

    window->show();
    return app->exec();
}
