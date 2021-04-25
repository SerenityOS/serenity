/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Music.h"
#include <AK/Types.h>
#include <LibCore/Object.h>

namespace LibDSP {

// The DAW-wide timekeeper and synchronizer
class Transport final : public Core::Object {
public:
    Transport(u16 beats_per_minute, u8 beats_per_measure)
        : m_beats_per_minute(beats_per_minute)
        , m_beats_per_measure(beats_per_measure)
    {
    }

    const u32& time() const { return m_time; }
    u16 beats_per_minute() const { return m_beats_per_minute; }
    double current_second() const { return m_time / sample_rate; }
    double samples_per_measure() const { return (1.0 / m_beats_per_minute) * 60.0 * sample_rate; }
    double current_measure() const { return m_time / samples_per_measure(); }

private:
    const u32 m_time { 0 };
    const u16 m_beats_per_minute;
    const u8 m_beats_per_measure;
};

}
