/*
 * Copyright (c) 2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/WavWriter.h>

namespace Audio {

ErrorOr<NonnullOwnPtr<WavWriter>> WavWriter::create_from_file(StringView path, int sample_rate, u16 num_channels, u16 bits_per_sample)
{
    auto wav_writer = TRY(adopt_nonnull_own_or_enomem(new (nothrow) WavWriter(sample_rate, num_channels, bits_per_sample)));
    TRY(wav_writer->set_file(path));
    return wav_writer;
}

WavWriter::WavWriter(int sample_rate, u16 num_channels, u16 bits_per_sample)
    : m_sample_rate(sample_rate)
    , m_num_channels(num_channels)
    , m_bits_per_sample(bits_per_sample)
{
}

WavWriter::~WavWriter()
{
    if (!m_finalized)
        finalize();
}

ErrorOr<void> WavWriter::set_file(StringView path)
{
    m_file = TRY(Core::File::open(path, Core::File::OpenMode::ReadWrite));
    TRY(m_file->seek(44, SeekMode::SetPosition));
    m_finalized = false;
    return {};
}

ErrorOr<void> WavWriter::write_samples(Span<Sample> samples)
{
    m_data_sz += samples.size() * sizeof(Sample);

    for (auto const& sample : samples) {
        // FIXME: This only really works for 16-bit samples.
        u16 left = static_cast<i16>(sample.left * static_cast<float>(1 << m_bits_per_sample));
        u16 right = static_cast<i16>(sample.right * static_cast<float>(1 << m_bits_per_sample));
        // FIXME: This ignores endianness.
        TRY(m_file->write_value(left));
        TRY(m_file->write_value(right));
    }

    return {};
}

void WavWriter::finalize()
{
    VERIFY(!m_finalized);
    m_finalized = true;

    if (m_file && m_file->is_open()) {
        auto result = [&]() -> ErrorOr<void> {
            TRY(m_file->seek(0, SeekMode::SetPosition));
            return TRY(write_header());
        }();

        if (result.is_error())
            dbgln("Failed to finalize WavWriter: {}", result.error());
        m_file->close();
    }
    m_data_sz = 0;
}

ErrorOr<void> WavWriter::write_header()
{
    // "RIFF"
    static u32 riff = 0x46464952;
    TRY(m_file->write_value(riff));

    // Size of data + (size of header - previous field - this field)
    u32 sz = m_data_sz + (44 - 4 - 4);
    TRY(m_file->write_value(sz));

    // "WAVE"
    static u32 wave = 0x45564157;
    TRY(m_file->write_value(wave));

    // "fmt "
    static u32 fmt_id = 0x20746D66;
    TRY(m_file->write_value(fmt_id));

    // Size of the next 6 fields
    static u32 fmt_size = 16;
    TRY(m_file->write_value(fmt_size));

    // 1 for PCM
    static u16 audio_format = 1;
    TRY(m_file->write_value(audio_format));

    TRY(m_file->write_value(m_num_channels));

    TRY(m_file->write_value(m_sample_rate));

    u32 byte_rate = m_sample_rate * m_num_channels * (m_bits_per_sample / 8);
    TRY(m_file->write_value(byte_rate));

    u16 block_align = m_num_channels * (m_bits_per_sample / 8);
    TRY(m_file->write_value(block_align));

    TRY(m_file->write_value(m_bits_per_sample));

    // "data"
    static u32 chunk_id = 0x61746164;
    TRY(m_file->write_value(chunk_id));

    TRY(m_file->write_value(m_data_sz));

    return {};
}

}
