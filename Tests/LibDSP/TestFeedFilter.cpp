/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibDSP/FeedFilter.h>
#include <LibDSP/Music.h>

template<size_t lookahead, size_t lookbehind>
using Filter = DSP::FeedFilter<int, lookahead, lookbehind>;
using Sample = DSP::Sample;

static constexpr size_t const chunk_size = 5;

TEST_CASE(noop)
{
    Filter<1, 0> noop_filter;
    noop_filter.set_feedforward_coefficients({ 1 });
    Array<int, chunk_size> input { 1, 2, 3, 4, 5 };
    Array<int, chunk_size> output {};
    auto output_span = output.span();
    noop_filter.filter(input.span(), output_span);

    for (size_t i = 0; i < chunk_size; ++i)
        EXPECT_EQ(input[i], output[i]);

    noop_filter.filter(input.span(), output_span);
    for (size_t i = 0; i < chunk_size; ++i)
        EXPECT_EQ(input[i], output[i]);
}

TEST_CASE(zero)
{
    Filter<1, 0> zero_filter;
    Array<int, chunk_size> input { 1, 2, 3, 4, 5 };
    Array<int, chunk_size> output {};
    auto output_span = output.span();
    zero_filter.filter(input.span(), output_span);

    for (size_t i = 0; i < chunk_size; ++i)
        EXPECT_EQ(output[i], 0);
}

TEST_CASE(delay)
{
    // Filter that delays the input signal by one sample.
    Filter<2, 0> delay_filter;
    delay_filter.set_feedforward_coefficients({ 0, 1 });
    Array<int, chunk_size> input { 1, 2, 3, 4, 5 };
    Array<int, chunk_size> output {};
    auto output_span = output.span();
    delay_filter.filter(input.span(), output_span);

    for (size_t i = 1; i < chunk_size; ++i)
        EXPECT_EQ(output[i], input[i - 1]);

    // Filter once more and check that I/O buffer works.
    delay_filter.filter(input.span(), output_span);
    EXPECT_EQ(output[0], input[chunk_size - 1]);
}

TEST_CASE(accumulative)
{
    // Filter that accumulatively sums samples.
    Filter<2, 2> accumulation_filter;
    accumulation_filter.set_feedforward_coefficients({ 0, 1 });
    accumulation_filter.set_feedback_coefficients({ 1, 1 });
    Array<int, chunk_size> input { 1, 1, 1, 1, 1 };
    Array<int, chunk_size> output {};
    auto output_span = output.span();
    accumulation_filter.filter(input.span(), output_span);

    for (size_t i = 0; i < chunk_size; ++i)
        EXPECT_EQ(output[i], static_cast<int>(i));

    accumulation_filter.filter(input.span(), output_span);
    for (size_t i = 0; i < chunk_size; ++i)
        EXPECT_EQ(output[i], static_cast<int>(i + chunk_size));
}

TEST_CASE(sample)
{
    DSP::FeedFilter<Sample, 1, 0> noop_filter;
    noop_filter.set_feedforward_coefficients({ Sample { 1.0f } });
    Array<Sample, chunk_size> input { Sample { 1.0f }, Sample { 2.0f }, Sample { 3.0f }, Sample { 4.0f }, Sample { 5.0f } };
    Array<Sample, chunk_size> output {};
    auto output_span = output.span();
    noop_filter.filter(input.span(), output_span);

    for (size_t i = 0; i < chunk_size; ++i)
        EXPECT_APPROXIMATE(input[i].left, output[i].left);

    noop_filter.filter(input.span(), output_span);
    for (size_t i = 0; i < chunk_size; ++i)
        EXPECT_APPROXIMATE(input[i].left, output[i].left);
}
