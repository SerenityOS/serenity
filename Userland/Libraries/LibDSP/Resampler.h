/*
 * Copyright (c) 2023, Sarsaparilla
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Math.h>
#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <AK/Vector.h>
#include <AK/TypedTransfer.h>
#include <LibC/math.h>
#include <LibDSP/FIRFilter.h>
#include <LibDSP/Window.h>

namespace DSP {

constexpr double normalized_sinc(double phi)
{
    if (AK::abs(phi) < NumericLimits<double>::epsilon()) {
        return 1.0;
    } else {
        phi *= AK::Pi<double>;
        return AK::sin(phi) / phi;
    }
}

class InterpolatedSinc {
public:
    static ErrorOr<InterpolatedSinc> create(size_t sinc_taps, size_t oversample)
    {
        // Oversample is the number of lookup values between the taps. The more, the less aliasing noise.
        // TODO: Use something better than linear interpolation so that we can save memory.

        // +1 because we want the taps to be the same amount to the left and to the right.
        // And another +1 because of an additional tap at (-TapCount - 1) to protect against bad lookups
        // coming from rounding errors.
        size_t const sinc_lookup_table_size = (2 * sinc_taps + 2) * oversample;

        Vector<double> sinc_table;
        TRY(sinc_table.try_resize(sinc_lookup_table_size));

        for (ssize_t k = -sinc_taps - 1; k <= static_cast<ssize_t>(sinc_taps); k++) {
            for (size_t i = 0; i < oversample; i++) {
                double const sinc_index = k + static_cast<double>(i) / static_cast<double>(oversample);
                size_t const window_index = (k + sinc_taps + 1) * oversample + i;
                double const window = Window<double>::blackman_harris(window_index, sinc_lookup_table_size);
                sinc_table[window_index] = normalized_sinc(sinc_index) * window;
            }
        }

        return InterpolatedSinc(move(sinc_table), sinc_taps, oversample);
    }

    double operator()(double phi) const
    {
        double const fraction = phi - static_cast<i32>(phi);

        // +1 to skip the rounding error buffer tap
        size_t const n = static_cast<size_t>(phi * m_sinc_lookup_oversample) + m_index_offset;

        double const y1 = m_sinc_table[n];
        double const y2 = m_sinc_table[n + 1];

        return y1 + fraction * (y2 - y1);
    }

private:
    InterpolatedSinc(Vector<double>&& table, size_t taps, size_t oversample)
        : m_sinc_table(table), m_sinc_taps(taps), m_sinc_lookup_oversample(oversample), m_index_offset((m_sinc_taps + 1) * m_sinc_lookup_oversample)
    {
    }

    Vector<double> m_sinc_table;
    size_t const m_sinc_taps;
    size_t const m_sinc_lookup_oversample;
    size_t const m_index_offset;
};

template<typename SampleType>
class SincResampler {
public:
    SincResampler(double ratio, Function<double(double)>&& sinc_function, size_t sinc_taps, Vector<SampleType>&& input_buffer, FIRFilter<SampleType, double>&& lowpass)
        : m_phase(0), m_sinc_taps(sinc_taps), m_processed_sample_count(0), m_ratio(ratio), m_sinc_function(move(sinc_function)), m_input_buffer_size(input_buffer.size()), m_input_buffer(move(input_buffer)), m_lowpass(move(lowpass))
    {
    }

    static ErrorOr<SincResampler> create(u32 rate_from, u32 rate_to, size_t max_input_buffer_size, Function<double(double)>&& sinc_function, size_t sinc_taps, double transition_bandwidth_hz) {
        double const ratio = rate_from / static_cast<double>(rate_to);

        // Cutoff frequency as a fraction of pi (1/2 is the nyquist frequency)
        double const cutoff = 1.0 / (2.0 * ratio);
        double const transition_bandwidth = transition_bandwidth_hz / static_cast<double>(rate_from);

        FIRFilter lowpass = TRY(calculate_lowpass(cutoff, transition_bandwidth));

        // The input buffer is a shifting window over all the input data.
        // When we shift the window to the right, we have to leave room for the previous
        // `sinc_taps` number of samples to be multiplied with the left side of the sinc.
        size_t const input_buffer_size = max_input_buffer_size + 2 * sinc_taps + 1;

        Vector<SampleType> input_buffer;
        TRY(input_buffer.try_resize(input_buffer_size));

        return SincResampler(ratio, move(sinc_function), sinc_taps, move(input_buffer), move(lowpass));
    }

    size_t process(ReadonlySpan<SampleType> input, Span<SampleType> output)
    {
        size_t const total_tap_count = 2 * m_sinc_taps + 1;
        Span<SampleType> old_data { &m_input_buffer[m_input_buffer.size() - total_tap_count], total_tap_count };
        Span<SampleType> new_data { &m_input_buffer[total_tap_count], input.size() };

        AK::TypedTransfer<SampleType>::move(m_input_buffer.data(), old_data.data(), old_data.size());
        AK::TypedTransfer<SampleType>::copy(new_data.data(), input.data(), input.size());
        m_input_buffer_size = old_data.size() + input.size();

        VERIFY(m_input_buffer_size <= m_input_buffer.size());

        if (m_ratio > 1)
            for (size_t i = 0; i < input.size(); i++)
                new_data[i] = static_cast<SampleType>(m_lowpass.process(new_data[i]));

        size_t samples_written = 0;
        size_t const input_buffer_start_sample_count = static_cast<size_t>(m_processed_sample_count * m_ratio);
        size_t sinc_center_index = m_sinc_taps;

        while (sinc_center_index + m_sinc_taps + 1 < m_input_buffer_size) {
            // Whittaker–Shannon interpolation formula
            SampleType sum {};
            for (ssize_t k = -m_sinc_taps; k <= static_cast<ssize_t>(m_sinc_taps); k++)
                sum += m_input_buffer[sinc_center_index + k] * m_sinc_function(m_phase - k);

            output[samples_written] = sum;

            samples_written++;
            m_processed_sample_count++;

            m_phase = m_processed_sample_count * m_ratio;
            m_phase = m_phase - static_cast<i32>(m_phase);

            sinc_center_index = m_sinc_taps + static_cast<size_t>(m_processed_sample_count * m_ratio) - input_buffer_start_sample_count;
        }

        return samples_written;
    }

private:
    static ErrorOr<FIRFilter<SampleType, double>> calculate_lowpass(double cutoff, double transition_bandwidth)
    {
        // The stopband should begin at the cutoff
        cutoff -= transition_bandwidth;

        // Just a rough estimate
        size_t taps = AK::round_to<size_t>(4 / transition_bandwidth);
        // Make taps symmetrical
        if (taps % 2 == 0)
            taps += 1;

        Vector<double> coefficients;
        TRY(coefficients.try_resize(taps));

        double sum = 0;
        for (size_t i = 0; i < taps; i++) {
            double const phi = 2 * cutoff * (i - (taps - 1) / 2.0);
            double const coefficient = normalized_sinc(phi) * Window<double>::blackman_harris(i, taps);

            sum += coefficient;
            coefficients[i] = coefficient;
        }

        for (size_t i = 0; i < taps; i++)
            coefficients[i] /= sum;

        return FIRFilter<SampleType, double>::create(coefficients);
    }

    double m_phase;
    size_t m_sinc_taps;
    size_t m_processed_sample_count;
    double m_ratio;
    Function<double(double)> m_sinc_function;
    Vector<double> m_sinc_table;
    size_t m_input_buffer_size;
    Vector<SampleType> m_input_buffer;
    FIRFilter<SampleType, double> m_lowpass;
};

}
