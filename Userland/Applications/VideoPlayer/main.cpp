/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    const auto& track = document->track_for_track_type(Video::TrackEntry::TrackType::Video).value();
    const auto& video_track = track.video_track().value();

    auto image = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA8888, Gfx::IntSize(video_track.pixel_height, video_track.pixel_width));
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
