/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibLocale/Segmenter.h>
#include <LibTest/TestCase.h>

constexpr size_t N = 10'000;

static auto make_string()
{
    return MUST(String::repeated("hello "_string, N));
}

auto long_string = make_string();

BENCHMARK_CASE(for_each_boundary)
{
    Vector<size_t> boundaries;
    auto segmenter = Locale::Segmenter::create(Locale::SegmenterGranularity::Word);

    segmenter->for_each_boundary(long_string, [&](auto boundary) {
        boundaries.append(boundary);
        return IterationDecision::Continue;
    });

    EXPECT_EQ(boundaries.size(), 2 * N + 1);
}

BENCHMARK_CASE(forward)
{
    Vector<size_t> boundaries;
    auto segmenter = Locale::Segmenter::create(Locale::SegmenterGranularity::Word);
    segmenter->set_segmented_text(long_string);

    size_t boundary = 0;
    boundaries.append(boundary);
    while (true) {
        auto next_boundary = segmenter->next_boundary(boundary);
        if (!next_boundary.has_value())
            break;

        boundary = next_boundary.value();
        boundaries.append(boundary);
    }

    EXPECT_EQ(boundaries.size(), 2 * N + 1);
}

BENCHMARK_CASE(backward)
{
    Vector<size_t> boundaries;
    auto segmenter = Locale::Segmenter::create(Locale::SegmenterGranularity::Word);
    segmenter->set_segmented_text(long_string);

    size_t boundary = long_string.bytes_as_string_view().length();
    boundaries.append(boundary);
    while (true) {
        auto next_boundary = segmenter->previous_boundary(boundary);
        if (!next_boundary.has_value())
            break;

        boundary = next_boundary.value();
        boundaries.append(boundary);
    }

    EXPECT_EQ(boundaries.size(), 2 * N + 1);
}
