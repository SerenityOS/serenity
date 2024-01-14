/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/DeltaE.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ICC/WellKnownProfiles.h>
#include <LibGfx/ImageFormats/JPEGLoader.h>
#include <LibGfx/ImageFormats/JPEGWriter.h>
#include <LibMain/Main.h>

struct Fixpoint {
    Gfx::Color fixpoint;
    int number_of_iterations {};
};

static ErrorOr<Fixpoint> compute_fixpoint(Gfx::Color start_color)
{
    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 8, 8 }));
    bitmap->fill(start_color);

    int number_of_iterations = 1;
    Color last_color = start_color;
    while (true) {
        AllocatingMemoryStream stream;
        TRY(Gfx::JPEGWriter::encode(stream, *bitmap));
        auto data = TRY(stream.read_until_eof());
        auto plugin_decoder = TRY(Gfx::JPEGImageDecoderPlugin::create(data));
        auto frame = TRY(plugin_decoder->frame(0));

        Color current_color = frame.image->get_pixel(4, 4);
        if (current_color == last_color)
            break;

        ++number_of_iterations;
        last_color = current_color;
        bitmap = *frame.image;
    }
    return Fixpoint { last_color, number_of_iterations };
}

static ErrorOr<float> perceived_distance_in_sRGB(Gfx::Color a, Gfx::Color b)
{
    auto sRGB = TRY(Gfx::ICC::sRGB());

    Array<u8, 3> array_a { a.red(), a.green(), a.blue() };
    Array<u8, 3> array_b { b.red(), b.green(), b.blue() };

    return DeltaE(TRY(sRGB->to_lab(array_a)), TRY(sRGB->to_lab(array_b)));
}

struct Stats {
    float max_delta {};
    int max_number_of_iterations {};
};

static ErrorOr<void> test(Gfx::Color color, Stats& stats)
{
    auto fixpoint = TRY(compute_fixpoint(color));

    float perceived_distance = TRY(perceived_distance_in_sRGB(color, fixpoint.fixpoint));

    outln("color {} converges to {} after saving {} times, delta {}", color, fixpoint.fixpoint, fixpoint.number_of_iterations, perceived_distance);

    stats.max_delta = max(stats.max_delta, perceived_distance);
    stats.max_number_of_iterations = max(stats.max_number_of_iterations, fixpoint.number_of_iterations);

    return {};
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    Stats stats;

    TRY(test(Gfx::Color::Red, stats));
    TRY(test(Gfx::Color::Green, stats));
    TRY(test(Gfx::Color::Blue, stats));

    TRY(test(Gfx::Color::LightBlue, stats));

    TRY(test(Gfx::Color::MidRed, stats));
    TRY(test(Gfx::Color::MidGreen, stats));
    TRY(test(Gfx::Color::MidBlue, stats));

    TRY(test(Gfx::Color::DarkRed, stats));
    TRY(test(Gfx::Color::DarkGreen, stats));
    TRY(test(Gfx::Color::DarkBlue, stats));

    TRY(test(Gfx::Color::Cyan, stats));
    TRY(test(Gfx::Color::Magenta, stats));
    TRY(test(Gfx::Color::Yellow, stats));

    TRY(test(Gfx::Color::Black, stats));
    TRY(test(Gfx::Color::DarkGray, stats));
    TRY(test(Gfx::Color::MidGray, stats));
    TRY(test(Gfx::Color::LightGray, stats));
    TRY(test(Gfx::Color::White, stats));

    outln();
    outln("max delta {}, max number of iterations {}", stats.max_delta, stats.max_number_of_iterations);

    return 0;
}
