/*
 * Copyright (c) 2020-2020, William McPherson <willmcpherson2@gmail.com>
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

#include <AK/StringView.h>
#include <LibCore/File.h>

namespace Audio {

class WavWriter {
public:
    WavWriter(const StringView& path, int sample_rate = 44100, int num_channels = 2, int bits_per_sample = 16);
    WavWriter(int sample_rate = 44100, int num_channels = 2, int bits_per_sample = 16);
    ~WavWriter();

    bool has_error() const { return !m_error_string.is_null(); }
    const char* error_string() const { return m_error_string.characters(); }

    void write_samples(const u8* samples, size_t size);
    void finalize(); // You can finalize manually or let the destructor do it.

    u32 sample_rate() const { return m_sample_rate; }
    u16 num_channels() const { return m_num_channels; }
    u16 bits_per_sample() const { return m_bits_per_sample; }
    RefPtr<Core::File> file() const { return m_file; }

    void set_file(const StringView& path);
    void set_num_channels(int num_channels) { m_num_channels = num_channels; }
    void set_sample_rate(int sample_rate) { m_sample_rate = sample_rate; }
    void set_bits_per_sample(int bits_per_sample) { m_bits_per_sample = bits_per_sample; }

    void clear_error() { m_error_string = String(); }

private:
    void write_header();
    RefPtr<Core::File> m_file;
    String m_error_string;
    bool m_finalized { false };

    u32 m_sample_rate;
    u16 m_num_channels;
    u16 m_bits_per_sample;
    u32 m_data_sz { 0 };
};

}
