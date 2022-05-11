/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Effects.h"
#include <AK/FixedArray.h>
#include <math.h>

namespace LibDSP::Effects {

Delay::Delay(NonnullRefPtr<Transport> transport)
    : EffectProcessor(move(transport))
    , m_delay_decay("Decay"sv, 0.01, 0.99, 0.33, Logarithmic::No)
    , m_delay_time("Delay Time"sv, 3, 2000, 900, Logarithmic::Yes)
    , m_dry_gain("Dry"sv, 0, 1, 0.9, Logarithmic::No)
{

    m_parameters.append(m_delay_decay);
    m_parameters.append(m_delay_time);
    m_parameters.append(m_dry_gain);
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
    // FIXME: This is allocating and needs to happen on a different thread.
    handle_delay_time_change();

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
{
}

void Mastering::process_impl([[maybe_unused]] Signal const& input_signal, [[maybe_unused]] Signal& output_signal)
{
    TODO();
}

}
