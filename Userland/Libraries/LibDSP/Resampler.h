/*
 * Copyright (c) 2023, Sarsaparilla
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Function.h>
#include <AK/Math.h>
#include <AK/NumericLimits.h>
#include <AK/Span.h>
#include <LibDSP/FIRFilter.h>
#include <LibDSP/Window.h>

namespace DSP {

constexpr float normalized_sinc(float phi)
{
    if (AK::abs(phi) < NumericLimits<float>::epsilon())
        return 1.0;

    phi *= AK::Pi<float>;
    return AK::sin(phi) / phi;
}

struct Sinc {
    constexpr float operator()(float phi) const { return normalized_sinc(phi); }
};

// Oversample is the number of lookup values between the taps. The more, the less aliasing noise.
// TODO: Use something better than linear interpolation so that we can save memory.
template<size_t sinc_taps_parameter, size_t oversample_parameter>
struct InterpolatedSinc {
public:
    static constexpr size_t sinc_taps = sinc_taps_parameter;
    static constexpr size_t oversample = oversample_parameter;

    static constexpr float oversample_float = static_cast<float>(oversample);

    // +1 to skip the rounding error buffer tap
    static constexpr size_t index_offset = (sinc_taps + 1) * oversample;

    // +1 because we want the taps to be the same amount to the left and to the right.
    // And another +1 because of an additional tap at (-TapCount - 1) to protect against bad lookups
    // coming from rounding errors.
    static constexpr size_t sinc_lookup_table_size = (2 * sinc_taps + 2) * oversample;
    static_assert(sinc_taps <= NumericLimits<ssize_t>::max());

    // FIXME: Make this constexpr once Clang understands constexpr cos().
    static Array<float, sinc_lookup_table_size> make_sinc_table()
    {
        Array<float, sinc_lookup_table_size> sinc_table;

        for (ssize_t k = -static_cast<ssize_t>(sinc_taps) - 1; k <= static_cast<ssize_t>(sinc_taps); k++) {
            for (size_t i = 0; i < oversample; i++) {
                float const sinc_index = static_cast<float>(k) + static_cast<float>(i) / oversample_float;
                size_t const window_index = (k + sinc_taps + 1) * oversample + i;
                float const window = Window<float>::blackman_harris(window_index, sinc_lookup_table_size);
                sinc_table[window_index] = normalized_sinc(sinc_index) * window;
            }
        }

        return sinc_table;
    }
    static inline Array<float, sinc_lookup_table_size> sinc_table { make_sinc_table() };

    float operator()(float phi) const
    {
        auto const fraction = phi - static_cast<float>(static_cast<i64>(phi));

        size_t const n = static_cast<size_t>(phi * oversample_float + index_offset);

        // Use check-free indexing since we know the index will always be in range.
        // That allows various high-impact optimizations (FMA and vectorizations) to kick in.
        auto const y1 = *sinc_table.span().offset_pointer(n);
        auto const y2 = *sinc_table.span().offset_pointer(n + 1);

        return y1 + fraction * (y2 - y1);
    }
};

template<typename SampleType, typename SincFunctionType>
requires(IsCallableWithArguments<SincFunctionType, float, float>)
class SincResampler {
public:
    using SincFunction = SincFunctionType;

    SincResampler(float ratio, size_t sinc_taps, FixedArray<SampleType>&& input_buffer, FIRFilter<SampleType, float>&& lowpass)
        : m_sinc_taps(sinc_taps)
        , m_ratio(ratio)
        , m_lowpass(move(lowpass))
        , m_input_buffer_size(input_buffer.size())
        , m_input_buffer(move(input_buffer))
    {
    }

    static ErrorOr<SincResampler> create(u32 rate_from, u32 rate_to, size_t max_input_buffer_size, size_t sinc_taps, float transition_bandwidth_hz)
    {
        float const ratio = rate_from / static_cast<float>(rate_to);

        // Cutoff frequency as a fraction of pi (1/2 is the nyquist frequency)
        float const cutoff = 1.0f / (2.0f * ratio);
        float const transition_bandwidth = transition_bandwidth_hz / static_cast<float>(rate_from);

        FIRFilter lowpass = TRY(calculate_lowpass(cutoff, transition_bandwidth));

        // The input buffer is a shifting window over all the input data.
        // When we shift the window to the right, we have to leave room for the previous
        // `sinc_taps` number of samples to be multiplied with the left side of the sinc.
        size_t const input_buffer_size = max_input_buffer_size + 2 * sinc_taps + 1;

        auto input_buffer = TRY(FixedArray<SampleType>::create(input_buffer_size));

        return SincResampler(ratio, sinc_taps, move(input_buffer), move(lowpass));
    }

    size_t process(ReadonlySpan<SampleType> input, Span<SampleType> output)
    {
        auto const total_tap_count = 2 * m_sinc_taps + 1;

        // We need some of the last samples for lookback, since the sinc interpolation considers samples on both sides of the center sample.
        // Therefore, copy the needed old samples to the start of the buffer, and insert the new samples after that.
        auto old_data = m_input_buffer.span().slice_from_end(total_tap_count);
        auto new_data = m_input_buffer.span().slice(total_tap_count, input.size());
        old_data.move_to(m_input_buffer.span());
        input.copy_trimmed_to(new_data);
        m_input_buffer_size = old_data.size() + input.size();

        VERIFY(m_input_buffer_size <= m_input_buffer.size());

        if (m_ratio > 1) {
            // Band-limit the signal to the target sample rate's Nyquist frequency to prevent aliasing.
            for (size_t i = 0; i < input.size(); i++)
                *new_data.offset_pointer(i) = static_cast<SampleType>(m_lowpass.process(*new_data.offset_pointer(i)));
        }

        // If the "output phase" wrapped around since the last write, we need to write one less sample to the output.
        // This accounts for the fact that our output write limit is effectively rounded up by nature of sinc_center_index,
        // and we would start pitch shifting for larger ratios.
        auto fractional_output_size = static_cast<double>(input.size()) / static_cast<double>(m_ratio);
        m_output_phase += fractional_output_size - static_cast<size_t>(fractional_output_size);
        // By default, we remove the rounding up by adding this extra limit to the loop.
        auto extra_input_limit = m_ratio;
        if (m_output_phase >= 1) {
            extra_input_limit = 0;
            m_output_phase -= 1;
        }

        size_t samples_written = 0;
        auto const input_buffer_start_sample_count = static_cast<size_t>(m_processed_sample_count * m_ratio);
        auto sinc_center_index = m_sinc_taps;

        auto const input_buffer_size_float = m_input_buffer_size;
        while (static_cast<float>(sinc_center_index + m_sinc_taps + 1) + extra_input_limit < input_buffer_size_float) {
            // Whittaker–Shannon interpolation formula, a convolution between the input buffer and the sinc kernel.
            SampleType sum {};
            for (ssize_t k = -m_sinc_taps; k <= static_cast<ssize_t>(m_sinc_taps); k++)
                sum += m_input_buffer.unchecked_at(sinc_center_index + k) * sinc_function(m_phase - static_cast<float>(k));

            output[samples_written] = sum;

            samples_written++;
            m_processed_sample_count++;

            // Current position within the input buffer, may lie between samples.
            m_phase = m_processed_sample_count * m_ratio;
            auto const next_sample = static_cast<size_t>(m_phase);
            // Convert the next sample position to an index matching our current buffer window.
            sinc_center_index = m_sinc_taps + next_sample - input_buffer_start_sample_count;
            // Remove the non-fractional part from the phase to reestablish the range [0, 1].
            m_phase = m_phase - static_cast<float>(next_sample);
        }

        return samples_written;
    }

    constexpr float ratio() const { return m_ratio; }
    constexpr float sinc_taps() const { return m_sinc_taps; }

private:
    static ErrorOr<FIRFilter<SampleType, float>> calculate_lowpass(float cutoff, float transition_bandwidth)
    {
        // The stopband should begin at the cutoff
        cutoff -= transition_bandwidth;

        // Just a rough estimate
        size_t taps = AK::round_to<size_t>(4.f / transition_bandwidth);
        // Make taps symmetrical
        if (taps % 2 == 0)
            taps += 1;

        auto coefficients = TRY(FixedArray<float>::create(taps));

        float sum = 0;
        for (size_t i = 0; i < taps; i++) {
            float const phi = 2.f * cutoff * (static_cast<float>(i) - static_cast<float>(taps - 1) / 2.f);
            float const coefficient = normalized_sinc(phi) * Window<float>::blackman_harris(i, taps);

            sum += coefficient;
            coefficients[i] = coefficient;
        }

        for (size_t i = 0; i < taps; i++)
            coefficients[i] /= sum;

        return FIRFilter<SampleType, float>::create(move(coefficients));
    }

    SincFunction const sinc_function {};

    float m_phase { 0.f };
    float m_output_phase { 0.f };
    size_t const m_sinc_taps;

    float const m_ratio;
    FIRFilter<SampleType, float> m_lowpass;

    size_t m_processed_sample_count { 0 };
    size_t m_input_buffer_size;
    FixedArray<SampleType> m_input_buffer;
};

template<typename SampleType, size_t sinc_taps, size_t oversample>
using InterpolatedSincResampler = SincResampler<SampleType, InterpolatedSinc<sinc_taps, oversample>>;

// Good parameters for float sample processing.
// Adapted from: https://ccrma.stanford.edu/~jos/resample/Implementation.html
// "As shown below, if n_c denotes the word-length of the stored impulse-response samples,
//  then one may choose n_l=1+n_c/2, and n_η=n_c/2 to obtain n_c-1 effective bits of precision in the interpolated impulse response."
// Since we are not using fixed point, in a first step we have to approximate the number of binary digits to the floating point mantissa: n_c=23.
// We obtain the tap count exponent n_l=12 (4096 taps) and the interpolation count exponent n_η=11 (2048 interpolation lookup values per tap).
// This would be slow, but gives an upper limit of what is even numerically sensible.
// Consider further that the limit of human hearing is around a range of 60dB.
// In 32-bit floating point, this corresponds to an epsilon of 10^{-60/20} = 0.001 ≈ 2^{-10}.
// Therefore, we need no higher precision than n_c=10, which gives n_l=6, n_η=5.
constexpr size_t recommended_float_sinc_taps = 1 << 6;
constexpr size_t recommended_float_oversample = 1 << 5;

}
