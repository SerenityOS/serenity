/*
 * Copyright (c) 2022-2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>

namespace DSP {

// A generic linear shift-invariant (LSI) filter system that computes sample data from previous output samples (feedback) and previous input samples (feedforward).
// This filter operates in Direct Form 1, meaning that in each step, previous/current inputs and previous/current outputs are directly multiplied with the filter coefficients and summed together.
// Usually, feedback amount (lookbehind) and feedforward amount (lookahead) are the same, though this is not a requirement.
template<typename SignalT, size_t lookahead, size_t lookbehind = lookahead>
class FeedFilter {
    static constexpr ssize_t signed_lookahead = static_cast<ssize_t>(lookahead);
    static constexpr ssize_t signed_lookbehind = static_cast<ssize_t>(lookbehind);

public:
    FeedFilter(Array<SignalT, lookahead> feedforward_coefficients, Array<SignalT, lookbehind> feedback_coefficients)
        : m_feedforward_coefficients(move(feedforward_coefficients))
        , m_feedback_coefficients(move(feedback_coefficients))
    {
    }
    FeedFilter() = default;

    void set_feedforward_coefficients(Array<SignalT, lookahead>&& feedforward_coefficients) { m_feedforward_coefficients = move(feedforward_coefficients); }
    void set_feedback_coefficients(Array<SignalT, lookbehind>&& feedback_coefficients) { m_feedback_coefficients = move(feedback_coefficients); }

    Array<SignalT, lookahead> const& feedforward_coefficients() const { return m_feedforward_coefficients; }
    Array<SignalT, lookbehind> const& feedback_coefficients() const { return m_feedback_coefficients; }
    Array<SignalT, lookahead> const& input_buffer() const { return m_input_buffer; }
    Array<SignalT, lookbehind> const& output_buffer() const { return m_output_buffer; }

    // Fills both buffers with the default signal.
    void clear()
    {
        m_input_buffer.fill({});
        m_output_buffer.fill({});
    }

    void filter(ReadonlySpan<SignalT> input, Span<SignalT> output_signal)
    {
        VERIFY(input.size() >= lookahead);
        VERIFY(output_signal.size() >= lookbehind);
        VERIFY(input.size() == output_signal.size());

        for (size_t i = 0; i < input.size(); ++i) {
            SignalT output {};
            // Lookahead contribution (from previous input samples).
            for (ssize_t offset = 0; offset < signed_lookahead; ++offset) {
                ssize_t index = static_cast<ssize_t>(i) - offset;
                auto const input_at_offset = (index < 0 ? m_input_buffer[lookahead + index] : input[index]);
                output += m_feedforward_coefficients[offset] * input_at_offset;
            }

            // Lookbehind contribution (from previous output samples).
            for (ssize_t offset = 1; offset < signed_lookbehind; ++offset) {
                ssize_t index = static_cast<ssize_t>(i) - offset;
                auto const output_at_offset = (index < 0 ? m_output_buffer[lookbehind + index] : output_signal[index]);
                // Direct Form 1 means that we subtract here.
                output -= m_feedback_coefficients[offset] * output_at_offset;
            }

            // Contribution of current sample's factor; often 1.
            if (lookbehind > 0)
                output *= m_feedback_coefficients[0];

            output_signal[i] = output;
        }

        // Store as much of the input and output as needed in the buffers.
        for (size_t offset = 0; offset < lookahead; ++offset)
            m_input_buffer[offset] = input[offset + (input.size() - lookahead)];
        for (size_t offset = 0; offset < lookbehind; ++offset)
            m_output_buffer[offset] = output_signal[offset + (output_signal.size() - lookbehind)];
    }

private:
    // These two terms make more sense in a system diagram.
    // Factors used with the previous input samples; zeroth factor is for this input sample.
    Array<SignalT, lookahead> m_feedforward_coefficients {};
    // Factors used with the previous output samples; zeroth factor is for this output sample.
    Array<SignalT, lookbehind> m_feedback_coefficients {};

    // Buffers for storing input and output data between calls to filter().
    Array<SignalT, lookahead> m_input_buffer {};
    Array<SignalT, lookbehind> m_output_buffer {};
};

template<size_t lookahead, size_t lookbehind = lookahead>
using SampleFeedFilter = FeedFilter<Sample, lookahead, lookbehind>;

}
