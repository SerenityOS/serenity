/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/MemoryStream.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Stream.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/WeakPtr.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/Loader.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>

namespace Audio {
class Buffer;

// defines for handling the WAV header data
#define WAVE_FORMAT_PCM 0x0001        // PCM
#define WAVE_FORMAT_IEEE_FLOAT 0x0003 // IEEE float
#define WAVE_FORMAT_ALAW 0x0006       // 8-bit ITU-T G.711 A-law
#define WAVE_FORMAT_MULAW 0x0007      // 8-bit ITU-T G.711 µ-law
#define WAVE_FORMAT_EXTENSIBLE 0xFFFE // Determined by SubFormat

// Parses a WAV file and produces an Audio::Buffer.
class WavLoaderPlugin : public LoaderPlugin {
public:
    WavLoaderPlugin(const StringView& path);
    WavLoaderPlugin(const ByteBuffer& buffer);

    virtual bool sniff() override { return valid; }

    virtual bool has_error() override { return !m_error_string.is_null(); }
    virtual const char* error_string() override { return m_error_string.characters(); }

    virtual RefPtr<Buffer> get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) override;

    virtual void reset() override { return seek(0); }
    virtual void seek(const int position) override;

    virtual int loaded_samples() override { return m_loaded_samples; }
    virtual int total_samples() override { return m_total_samples; }
    virtual u32 sample_rate() override { return m_sample_rate; }
    virtual u16 num_channels() override { return m_num_channels; }
    virtual PcmSampleFormat pcm_format() override { return m_sample_format; }
    virtual RefPtr<Core::File> file() override { return m_file; }

private:
    bool parse_header();

    bool valid { false };
    RefPtr<Core::File> m_file;
    OwnPtr<AK::InputStream> m_stream;
    AK::InputMemoryStream* m_memory_stream;
    String m_error_string;
    OwnPtr<ResampleHelper> m_resampler;

    u32 m_sample_rate { 0 };
    u16 m_num_channels { 0 };
    PcmSampleFormat m_sample_format;

    int m_loaded_samples { 0 };
    int m_total_samples { 0 };
};

}
