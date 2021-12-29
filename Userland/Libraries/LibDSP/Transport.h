/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibCore/Object.h>
#include <LibDSP/Music.h>

namespace LibDSP {

// The DAW-wide timekeeper and synchronizer
class Transport final : public Core::Object {
    C_OBJECT(Transport)
public:
    enum class Looping : bool {
        Disabled,
        Enabled
    };

    // This is the internal "fake" time: it always increases,
    // which greatly simplifies time-dependent calculations that therefore don't have to deal with looping.
    constexpr u32& time() { return m_time; }
    constexpr u32 time() const { return m_time; }
    // This is the real time: it loops and is displayed to the user.
    // It's also used when deciding which samples and notes to play.
    constexpr u32 looping_time() const
    {
        if (m_loop_state == Looping::Disabled)
            return m_time;

        if (m_time < m_loop_start)
            return m_time;
        return (m_time - m_loop_start) % loop_duration() + m_loop_start;
    }
    // Difference between real and fake time.
    constexpr u32 time_offset() const { return m_time - looping_time(); }
    // The user-facing second based on the looping time.
    constexpr double current_second() const { return static_cast<double>(looping_time()) / m_sample_rate; }
    // The user-facing measure based on the looping time.
    constexpr double current_measure() const { return looping_time() / samples_per_measure(); }

    constexpr u16 beats_per_minute() const { return m_beats_per_minute; }
    constexpr u8 beats_per_measure() const { return m_beats_per_measure; }
    constexpr double samples_per_measure() const { return (1.0 / m_beats_per_minute) * 60.0 * m_sample_rate; }
    constexpr double sample_rate() const { return m_sample_rate; }
    constexpr double ms_sample_rate() const { return m_sample_rate / 1000.; }

    constexpr Looping loop_state() const { return m_loop_state; }
    constexpr u32 loop_start() const { return m_loop_start; }
    constexpr u32 loop_end() const { return m_loop_end; }
    constexpr u32 loop_duration() const { return m_loop_end - m_loop_start; }

    void set_loop(u32 loop_start, u32 loop_end, Looping loop_state = Looping::Enabled)
    {
        VERIFY(loop_end >= loop_start);
        m_loop_start = loop_start;
        m_loop_end = loop_end;
        enable_looping(loop_state);
    }
    // This can also disable looping
    void enable_looping(Looping loop_state = Looping::Enabled)
    {
        if (loop_duration() != 0)
            m_loop_state = loop_state;
        else
            m_loop_state = Looping::Disabled;
    }

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

    // FIXME: You can't make more than 24h of (48kHz) music with this.
    // But do you want to, really? :^)
    u32 m_time { 0 };
    u32 m_loop_start { 0 };
    u32 m_loop_end { 0 };
    Looping m_loop_state { Looping::Disabled };

    u16 const m_beats_per_minute { 0 };
    u8 const m_beats_per_measure { 0 };
    u32 const m_sample_rate;
};

}
