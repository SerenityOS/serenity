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

#include <LibAudio/WavWriter.h>

namespace Audio {

WavWriter::WavWriter(const StringView& path, int sample_rate, int num_channels, int bits_per_sample)
    : m_sample_rate(sample_rate)
    , m_num_channels(num_channels)
    , m_bits_per_sample(bits_per_sample)
{
    set_file(path);
}

WavWriter::WavWriter(int sample_rate, int num_channels, int bits_per_sample)
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

void WavWriter::set_file(const StringView& path)
{
    m_file = Core::File::construct(path);
    if (!m_file->open(Core::IODevice::ReadWrite)) {
        m_error_string = String::format("Can't open file: %s", m_file->error_string());
        return;
    }
    m_file->seek(44);
    m_finalized = false;
}

void WavWriter::write_samples(const u8* samples, size_t size)
{
    m_data_sz += size;
    m_file->write(samples, size);
}

void WavWriter::finalize()
{
    ASSERT(!m_finalized);
    m_finalized = true;
    m_file->seek(0);
    write_header();
    m_file->close();
    m_data_sz = 0;
}

void WavWriter::write_header()
{
    // "RIFF"
    static u32 riff = 0x46464952;
    m_file->write(reinterpret_cast<u8*>(&riff), sizeof(riff));

    // Size of data + (size of header - previous field - this field)
    u32 sz = m_data_sz + (44 - 4 - 4);
    m_file->write(reinterpret_cast<u8*>(&sz), sizeof(sz));

    // "WAVE"
    static u32 wave = 0x45564157;
    m_file->write(reinterpret_cast<u8*>(&wave), sizeof(wave));

    // "fmt "
    static u32 fmt_id = 0x20746D66;
    m_file->write(reinterpret_cast<u8*>(&fmt_id), sizeof(fmt_id));

    // Size of the next 6 fields
    static u32 fmt_size = 16;
    m_file->write(reinterpret_cast<u8*>(&fmt_size), sizeof(fmt_size));

    // 1 for PCM
    static u16 audio_format = 1;
    m_file->write(reinterpret_cast<u8*>(&audio_format), sizeof(audio_format));

    m_file->write(reinterpret_cast<u8*>(&m_num_channels), sizeof(m_num_channels));

    m_file->write(reinterpret_cast<u8*>(&m_sample_rate), sizeof(m_sample_rate));

    u32 byte_rate = m_sample_rate * m_num_channels * (m_bits_per_sample / 8);
    m_file->write(reinterpret_cast<u8*>(&byte_rate), sizeof(byte_rate));

    u16 block_align = m_num_channels * (m_bits_per_sample / 8);
    m_file->write(reinterpret_cast<u8*>(&block_align), sizeof(block_align));

    m_file->write(reinterpret_cast<u8*>(&m_bits_per_sample), sizeof(m_bits_per_sample));

    // "data"
    static u32 chunk_id = 0x61746164;
    m_file->write(reinterpret_cast<u8*>(&chunk_id), sizeof(chunk_id));

    m_file->write(reinterpret_cast<u8*>(&m_data_sz), sizeof(m_data_sz));
}

}
