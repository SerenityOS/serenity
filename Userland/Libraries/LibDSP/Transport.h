/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/Types.h>
#include <LibCore/Object.h>

namespace LibDSP {

// The DAW-wide timekeeper and synchronizer
class Transport final : public Core::Object {
    C_OBJECT(Transport)
public:
    u32 const& time() const { return m_time; }
    u16 beats_per_minute() const { return m_beats_per_minute; }
    double current_second() const { return m_time / m_sample_rate; }
    double samples_per_measure() const { return (1.0 / m_beats_per_minute) * 60.0 * m_sample_rate; }
    double sample_rate() const { return m_sample_rate; }
    double current_measure() const { return m_time / samples_per_measure(); }

private:
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

    u32 m_time { 0 };
    u16 const m_beats_per_minute { 0 };
    u8 const m_beats_per_measure { 0 };
    u32 const m_sample_rate;
};

}
