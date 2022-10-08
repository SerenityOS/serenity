/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

    auto document = Video::MatroskaReader::parse_matroska_from_file(filename);
    // FIXME: MatroskaReader should use ErrorOr
    if (!document) {
        outln("{} could not be read", filename);
        return 1;
    }
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

        auto result = vp9_decoder.receive_sample(optional_sample.release_value());

        if (result.is_error()) {
            outln("Error decoding frame {}: {}", frame_number, result.error().string_literal());
            return;
        }

        auto frame_result = vp9_decoder.get_decoded_frame();
        if (frame_result.is_error()) {
            outln("Error retrieving frame {}: {}", frame_number, frame_result.error().string_literal());
            return;
        }
        auto frame = frame_result.release_value();

        auto& cicp = frame->cicp();
        video_track.color_format.replace_code_points_if_specified(cicp);
        cicp.default_code_points_if_unspecified(Video::ColorPrimaries::BT709, Video::TransferCharacteristics::BT709, Video::MatrixCoefficients::BT709);

        auto convert_result = frame->output_to_bitmap(image);
        if (convert_result.is_error()) {
            outln("Error creating bitmap for frame {}: {}", frame_number, convert_result.error().string_literal());
            return;
        }

        image_widget->set_bitmap(image);
        image_widget->update();

        frame_index++;
        frame_number++;
    };

    image_widget->set_fixed_size(video_track.pixel_width, video_track.pixel_height);
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
