/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>

namespace DSP {

// A generic LSI filter system that computes sample data from previous output samples (feedback) and previous input samples (feedforward).
// Usually, feedback amount (lookbehind) and feedforward amount (lookahead) are the same, though this is not a requirement.
template<typename SignalT, size_t lookahead, size_t lookbehind = lookahead>
class FeedFilter {
public:
    FeedFilter(Array<SignalT, lookahead> feedforward_coefficients, Array<SignalT, lookbehind> feedback_coefficients)
        : m_feedforward_coefficients(move(feedforward_coefficients))
        , m_feedback_coefficients(move(feedback_coefficients))
    {
    }
    FeedFilter() = default;

    void set_feedforward_coefficients(Array<SignalT, lookahead> feedforward_coefficients) { m_feedforward_coefficients = feedforward_coefficients; }
    void set_feedback_coefficients(Array<SignalT, lookbehind> feedback_coefficients) { m_feedback_coefficients = feedback_coefficients; }

    Array<SignalT, lookahead> const& feedforward_coefficients() const { return m_feedforward_coefficients; }
    Array<SignalT, lookbehind> const& feedback_coefficients() const { return m_feedback_coefficients; }

    // Fills both buffers with the default signal.
    void clear()
    {
        m_input_buffer.fill({});
        m_output_buffer.fill({});
    }

    void filter(Span<SignalT> const& input, Span<SignalT>& output_signal)
    {
        VERIFY(input.size() >= lookahead);
        VERIFY(output_signal.size() >= lookbehind);
        VERIFY(input.size() == output_signal.size());

        for (size_t i = 0; i < input.size(); ++i) {
            SignalT output {};
            for (size_t offset = 0; offset < lookahead; ++offset) {
                ssize_t index = static_cast<ssize_t>(i) - offset;
                output += m_feedforward_coefficients[offset] * (index < 0 ? m_input_buffer[lookahead + index] : input[index]);
            }

            for (size_t offset = 1; offset < lookbehind; ++offset) {
                ssize_t index = static_cast<ssize_t>(i) - offset;
                output += m_feedback_coefficients[offset] * (index < 0 ? m_output_buffer[lookbehind + index] : output_signal[index]);
            }

            if (lookbehind > 0)
                output /= m_feedback_coefficients[0];

            output_signal[i] = output;
        }

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
