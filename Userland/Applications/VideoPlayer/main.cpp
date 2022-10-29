/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LibVideo/Color/CodingIndependentCodePoints.h"
#include "LibVideo/MatroskaDemuxer.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibMain/Main.h>
#include <LibVideo/Color/ColorConverter.h>
#include <LibVideo/MatroskaReader.h>
#include <LibVideo/VP9/Decoder.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool benchmark = false;
    StringView filename = "/home/anon/Videos/test-webm.webm"sv;

    Core::ArgsParser args_parser;
    args_parser.add_option(benchmark, "Benchmark the video decoder.", "benchmark", 'b');
    args_parser.add_positional_argument(filename, "The video file to display.", "filename", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::try_create(arguments));
    auto window = TRY(GUI::Window::try_create());

    auto demuxer_result = Video::MatroskaDemuxer::from_file(filename);
    if (demuxer_result.is_error()) {
        outln("Error parsing Matroska: {}", demuxer_result.release_error().string_literal());
        return 1;
    }
    auto demuxer = demuxer_result.release_value();
    auto tracks = demuxer->get_tracks_for_type(Video::TrackType::Video);
    if (tracks.is_empty()) {
        outln("No video tracks present.");
        return 1;
    }
    auto track = tracks[0];

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();
    auto image_widget = TRY(main_widget->try_add<GUI::ImageWidget>());

    OwnPtr<Video::VideoDecoder> decoder = make<Video::VP9::Decoder>();
    auto frame_number = 0u;

    auto display_next_frame = [&]() {
        auto sample_result = demuxer->get_next_video_sample_for_track(track);

        if (sample_result.is_error()) {
            outln("Error demuxing next sample {}: {}", frame_number, sample_result.release_error().string_literal());
            return;
        }

        auto sample = sample_result.release_value();
        auto result = decoder->receive_sample(sample->data());

        if (result.is_error()) {
            outln("Error decoding frame {}: {}", frame_number, result.error().string_literal());
            return;
        }

        auto frame_result = decoder->get_decoded_frame();
        if (frame_result.is_error()) {
            outln("Error retrieving frame {}: {}", frame_number, frame_result.error().string_literal());
            return;
        }
        auto frame = frame_result.release_value();

        auto& cicp = frame->cicp();
        cicp.adopt_specified_values(sample->container_cicp());
        cicp.default_code_points_if_unspecified({ Video::ColorPrimaries::BT709, Video::TransferCharacteristics::BT709, Video::MatrixCoefficients::BT709, Video::ColorRange::Studio });

        auto convert_result = frame->to_bitmap();
        if (convert_result.is_error()) {
            outln("Error creating bitmap for frame {}: {}", frame_number, convert_result.error().string_literal());
            return;
        }

        image_widget->set_bitmap(convert_result.release_value());
        image_widget->set_fixed_size(frame->size());
        image_widget->update();

        frame_number++;
    };

    image_widget->on_click = [&]() { display_next_frame(); };

    if (benchmark) {
        auto timer = Core::ElapsedTimer::start_new();
        for (auto i = 0; i < 100; i++)
            display_next_frame();
        auto elapsed_time = timer.elapsed_time();
        outln("Decoding 100 frames took {} ms", elapsed_time.to_milliseconds());
        return 0;
    }

    display_next_frame();

    window->show();
    return app->exec();
}
