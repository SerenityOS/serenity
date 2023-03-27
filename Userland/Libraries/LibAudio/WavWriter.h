/*
 * Copyright (c) 2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <LibAudio/Sample.h>
#include <LibCore/File.h>
#include <LibCore/Forward.h>

namespace Audio {

class WavWriter {
    AK_MAKE_NONCOPYABLE(WavWriter);
    AK_MAKE_NONMOVABLE(WavWriter);

public:
    static ErrorOr<NonnullOwnPtr<WavWriter>> create_from_file(StringView path, int sample_rate = 44100, u16 num_channels = 2, u16 bits_per_sample = 16);
    WavWriter(int sample_rate = 44100, u16 num_channels = 2, u16 bits_per_sample = 16);
    ~WavWriter();

    ErrorOr<void> write_samples(Span<Sample> samples);
    void finalize(); // You can finalize manually or let the destructor do it.

    u32 sample_rate() const { return m_sample_rate; }
    u16 num_channels() const { return m_num_channels; }
    u16 bits_per_sample() const { return m_bits_per_sample; }
    Core::File& file() const { return *m_file; }

    ErrorOr<void> set_file(StringView path);
    void set_num_channels(int num_channels) { m_num_channels = num_channels; }
    void set_sample_rate(int sample_rate) { m_sample_rate = sample_rate; }
    void set_bits_per_sample(int bits_per_sample) { m_bits_per_sample = bits_per_sample; }

private:
    ErrorOr<void> write_header();
    OwnPtr<Core::File> m_file;
    bool m_finalized { false };

    u32 m_sample_rate;
    u16 m_num_channels;
    u16 m_bits_per_sample;
    u32 m_data_sz { 0 };
};

}
