/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Effects.h"
#include <AK/FixedArray.h>
#include <math.h>

namespace DSP::Effects {

Delay::Delay(NonnullRefPtr<Transport> transport)
    : EffectProcessor(move(transport))
    , m_delay_decay("Decay"_string, 0.01, 0.99, 0.33, Logarithmic::No)
    , m_delay_time("Delay Time"_string, 3, 2000, 900, Logarithmic::Yes)
    , m_dry_gain("Dry"_string, 0, 1, 0.9, Logarithmic::No)
{

    m_parameters.append(m_delay_decay);
    m_parameters.append(m_delay_time);
    m_parameters.append(m_dry_gain);

    m_delay_time.register_change_listener([this](auto const&) {
        this->handle_delay_time_change();
    });
    handle_delay_time_change();
}

void Delay::handle_delay_time_change()
{
    // We want a delay buffer that can hold samples filling the specified number of milliseconds.
    double seconds = static_cast<double>(m_delay_time) / 1000.0;
    size_t sample_count = ceil(seconds * m_transport->sample_rate());
    if (sample_count != m_delay_buffer.size()) {
        m_delay_buffer.resize(sample_count, true);
        m_delay_index %= max(m_delay_buffer.size(), 1);
    }
}

void Delay::process_impl(Signal const& input_signal, Signal& output_signal)
{
    auto const& samples = input_signal.get<FixedArray<Sample>>();
    auto& output = output_signal.get<FixedArray<Sample>>();
    for (size_t i = 0; i < output.size(); ++i) {
        auto& out = output[i];
        auto const& sample = samples[i];
        out += sample.log_multiplied(static_cast<double>(m_dry_gain));
        out += m_delay_buffer[m_delay_index].log_multiplied(m_delay_decay);

        // This is also convenient for disabling the delay effect by setting the buffer size to 0
        if (m_delay_buffer.size() >= 1)
            m_delay_buffer[m_delay_index++] = out;

        if (m_delay_index >= m_delay_buffer.size())
            m_delay_index = 0;
    }
}

Mastering::Mastering(NonnullRefPtr<Transport> transport)
    : EffectProcessor(move(transport))
    , m_pan("Pan"_string, -1, 1, 0, Logarithmic::No)
    , m_volume("Volume"_string, 0, 1, 1, Logarithmic::No)
    , m_muted("Mute"_string, false)
{
    m_parameters.append(m_muted);
    m_parameters.append(m_volume);
    m_parameters.append(m_pan);
}

void Mastering::process_impl(Signal const& input_signal, Signal& output)
{
    process_to_fixed_array(input_signal, output.get<FixedArray<Sample>>());
}

void Mastering::process_to_fixed_array(Signal const& input_signal, FixedArray<Sample>& output)
{
    if (m_muted) {
        output.fill_with({});
        return;
    }

    auto const& input = input_signal.get<FixedArray<Sample>>();
    for (size_t i = 0; i < input.size(); ++i) {
        auto sample = input[i];
        sample.log_multiply(static_cast<float>(m_volume));
        sample.pan(static_cast<float>(m_pan));
        output[i] = sample;
    }
}

}
