/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <LibVideo/MatroskaReader.h>
#include <LibVideo/VP9/Decoder.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));
    auto window = TRY(GUI::Window::try_create());

    auto document = Video::MatroskaReader::parse_matroska_from_file("/home/anon/Videos/test-webm.webm"sv);
    auto const& optional_track = document->track_for_track_type(Video::TrackEntry::TrackType::Video);
    if (!optional_track.has_value())
        return 1;
    auto const& track = optional_track.value();
    auto const video_track = track.video_track().value();

    auto image = TRY(Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, Gfx::IntSize(video_track.pixel_width, video_track.pixel_height)));

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();
    auto image_widget = TRY(main_widget->try_add<GUI::ImageWidget>());

    Video::VP9::Decoder vp9_decoder;
    size_t cluster_index = 0;
    size_t block_index = 0;
    size_t frame_index = 0;
    auto frame_number = 0u;

    auto get_next_sample = [&]() -> Optional<ByteBuffer> {
        for (; cluster_index < document->clusters().size(); cluster_index++) {
            for (; block_index < document->clusters()[cluster_index].blocks().size(); block_index++) {
                auto const& candidate_block = document->clusters()[cluster_index].blocks()[block_index];
                if (candidate_block.track_number() != track.track_number())
                    continue;
                if (frame_index < candidate_block.frames().size())
                    return candidate_block.frame(frame_index);
                frame_index = 0;
            }
            block_index = 0;
        }
        return {};
    };

    auto display_next_frame = [&]() {
        auto optional_sample = get_next_sample();

        if (!optional_sample.has_value())
            return;

        auto result = vp9_decoder.decode_frame(optional_sample.release_value());

        if (result.is_error()) {
            outln("Error decoding frame {}: {}", frame_number, result.error().string_literal());
            return;
        }

        // FIXME: This method of output is temporary and should be replaced with an image struct
        //        containing the planes and their sizes. Ideally, this struct would be interpreted
        //        by some color conversion library and then passed to something (GL?) for output.
        auto const& output_y = vp9_decoder.get_output_buffer_for_plane(0);
        auto const& output_u = vp9_decoder.get_output_buffer_for_plane(1);
        auto const& output_v = vp9_decoder.get_output_buffer_for_plane(2);
        auto y_size = vp9_decoder.get_y_plane_size();
        auto uv_subsampling_y = vp9_decoder.get_uv_subsampling_y();
        auto uv_subsampling_x = vp9_decoder.get_uv_subsampling_x();
        Gfx::IntSize uv_size { y_size.width() >> uv_subsampling_x, y_size.height() >> uv_subsampling_y };

        for (auto y_row = 0u; y_row < video_track.pixel_height; y_row++) {
            auto uv_row = y_row >> uv_subsampling_y;

            for (auto y_column = 0u; y_column < video_track.pixel_width; y_column++) {
                auto uv_column = y_column >> uv_subsampling_x;

                auto y = output_y[y_row * y_size.width() + y_column];
                auto cb = output_u[uv_row * uv_size.width() + uv_column];
                auto cr = output_v[uv_row * uv_size.width() + uv_column];
                // Convert from Rec.709 YCbCr to RGB.
                auto r_float = floorf(clamp(y + (cr - 128) * 219.0f / 224.0f * 1.5748f, 0, 255));
                auto g_float = floorf(clamp(y + (cb - 128) * 219.0f / 224.0f * -0.0722f * 1.8556f / 0.7152f + (cr - 128) * 219.0f / 224.0f * -0.2126f * 1.5748f / 0.7152f, 0, 255));
                auto b_float = floorf(clamp(y + (cb - 128) * 219.0f / 224.0f * 1.8556f, 0, 255));
                auto r = static_cast<u8>(r_float);
                auto g = static_cast<u8>(g_float);
                auto b = static_cast<u8>(b_float);

                image->set_pixel(y_column, y_row, Gfx::Color(r, g, b));
            }
        }

        image_widget->set_bitmap(image);
        image_widget->update();

        frame_index++;
        frame_number++;
    };

    display_next_frame();

    window->show();
    return app->exec();
}
