/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/ClientConnection.h>
#include <LibCore/EventLoop.h>
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
    const auto& optional_track = document->track_for_track_type(Video::TrackEntry::TrackType::Video);
    if (!optional_track.has_value())
        return 1;
    const auto& track = optional_track.value();
    const auto video_track = track.video_track().value();

    auto image = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, Gfx::IntSize(video_track.pixel_height, video_track.pixel_width));
    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    auto& image_widget = main_widget.add<GUI::ImageWidget>();
    image_widget.set_bitmap(image);
    image_widget.set_fixed_size(video_track.pixel_height, video_track.pixel_width);
    main_widget.add_child(image_widget);

    Video::VP9::Decoder vp9_decoder;
    for (const auto& cluster : document->clusters()) {
        for (const auto& block : cluster.blocks()) {
            if (block.track_number() != track.track_number())
                continue;

            const auto& frame = block.frame(0);
            dbgln("Reading frame 0 from block @ {}", block.timestamp());
            vp9_decoder.parse_frame(frame);
            vp9_decoder.dump_info();
        }
    }

    window->show();
    return app->exec();
}
