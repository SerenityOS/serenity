/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>

namespace DSP {

// The DAW-wide timekeeper and synchronizer
class Transport final : public RefCounted<Transport> {
public:
    constexpr u32 time() const { return m_time; }
    constexpr u16 beats_per_minute() const { return m_beats_per_minute; }
    constexpr double current_second() const { return static_cast<double>(m_time) / m_sample_rate; }
    constexpr double samples_per_measure() const { return (1.0 / m_beats_per_minute) * 60.0 * m_sample_rate; }
    constexpr double sample_rate() const { return m_sample_rate; }
    constexpr double ms_sample_rate() const { return m_sample_rate / 1000.; }
    constexpr double current_measure() const { return m_time / samples_per_measure(); }

    void set_time(u32 time) { m_time = time; }

    Transport(u16 beats_per_minute, u8 beats_per_measure, u32 sample_rate)
        : m_beats_per_minute(beats_per_minute)
        , m_beats_per_measure(beats_per_measure)
        , m_sample_rate(sample_rate)
    {
    }
    Transport(u16 beats_per_minute, u8 beats_per_measure)
        : Transport(beats_per_minute, beats_per_measure, 44100)
    {
    }

private:
    // FIXME: You can't make more than 24h of (48kHz) music with this.
    // But do you want to, really? :^)
    u32 m_time { 0 };
    u16 const m_beats_per_minute { 0 };
    u8 const m_beats_per_measure { 0 };
    u32 const m_sample_rate;
};

}
