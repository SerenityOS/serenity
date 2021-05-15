/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/NumericLimits.h>
#include <AK/OwnPtr.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/WavLoader.h>
#include <LibCore/File.h>
#include <LibCore/IODeviceStreamReader.h>

namespace Audio {

static constexpr size_t maximum_wav_size = 1 * GiB; // FIXME: is there a more appropriate size limit?

WavLoaderPlugin::WavLoaderPlugin(const StringView& path)
    : m_file(Core::File::construct(path))
{
    if (!m_file->open(Core::OpenMode::ReadOnly)) {
        m_error_string = String::formatted("Can't open file: {}", m_file->error_string());
        return;
    }

    valid = parse_header();
    if (!valid)
        return;

    m_resampler = make<ResampleHelper>(m_sample_rate, 44100);
}

WavLoaderPlugin::WavLoaderPlugin(const ByteBuffer& buffer)
{
    m_stream = make<InputMemoryStream>(buffer);
    if (!m_stream) {
        m_error_string = String::formatted("Can't open memory stream");
        return;
    }

    valid = parse_header();
    if (!valid)
        return;

    m_resampler = make<ResampleHelper>(m_sample_rate, 44100);
}

bool WavLoaderPlugin::sniff()
{
    return valid;
}

RefPtr<Buffer> WavLoaderPlugin::get_more_samples(size_t max_bytes_to_read_from_input)
{
    dbgln_if(AWAVLOADER_DEBUG, "Read {} bytes WAV with num_channels {} sample rate {}, "
                               "bits per sample {}, sample format {}",
        max_bytes_to_read_from_input, m_num_channels,
        m_sample_rate, pcm_bits_per_sample(m_sample_format), sample_format_name(m_sample_format));
    size_t samples_to_read = static_cast<int>(max_bytes_to_read_from_input) / (m_num_channels * (pcm_bits_per_sample(m_sample_format) / 8));
    RefPtr<Buffer> buffer;
    if (m_file) {
        auto raw_samples = m_file->read(max_bytes_to_read_from_input);
        if (raw_samples.is_empty()) {
            return nullptr;
        }
        buffer = Buffer::from_pcm_data(raw_samples, *m_resampler, m_num_channels, m_sample_format);
    } else {
        buffer = Buffer::from_pcm_stream(*m_stream, *m_resampler, m_num_channels, m_sample_format, samples_to_read);
    }
    //Buffer contains normalized samples, but m_loaded_samples should contain the amount of actually loaded samples
    m_loaded_samples += samples_to_read;
    m_loaded_samples = min(m_total_samples, m_loaded_samples);
    return buffer;
}

void WavLoaderPlugin::seek(const int position)
{
    if (position < 0 || position > m_total_samples)
        return;

    m_loaded_samples = position;
    size_t byte_position = position * m_num_channels * (pcm_bits_per_sample(m_sample_format) / 8);

    if (m_file)
        m_file->seek(byte_position);
    else
        m_stream->seek(byte_position);
}

void WavLoaderPlugin::reset()
{
    seek(0);
}

bool WavLoaderPlugin::parse_header()
{
    OwnPtr<Core::IODeviceStreamReader> file_stream;
    bool ok = true;

    if (m_file)
        file_stream = make<Core::IODeviceStreamReader>(*m_file);

    auto read_u8 = [&]() -> u8 {
        u8 value;
        if (m_file) {
            *file_stream >> value;
            if (file_stream->handle_read_failure())
                ok = false;
        } else {
            *m_stream >> value;
            if (m_stream->handle_any_error())
                ok = false;
        }
        return value;
    };

    auto read_u16 = [&]() -> u16 {
        u16 value;
        if (m_file) {
            *file_stream >> value;
            if (file_stream->handle_read_failure())
                ok = false;
        } else {
            *m_stream >> value;
            if (m_stream->handle_any_error())
                ok = false;
        }
        return value;
    };

    auto read_u32 = [&]() -> u32 {
        u32 value;
        if (m_file) {
            *file_stream >> value;
            if (file_stream->handle_read_failure())
                ok = false;
        } else {
            *m_stream >> value;
            if (m_stream->handle_any_error())
                ok = false;
        }
        return value;
    };

#define CHECK_OK(msg)                                                      \
    do {                                                                   \
        if (!ok) {                                                         \
            m_error_string = String::formatted("Parsing failed: {}", msg); \
            return {};                                                     \
        }                                                                  \
    } while (0)

    u32 riff = read_u32();
    ok = ok && riff == 0x46464952; // "RIFF"
    CHECK_OK("RIFF header");

    u32 sz = read_u32();
    ok = ok && sz < 1024 * 1024 * 1024; // arbitrary
    CHECK_OK("File size");

    u32 wave = read_u32();
    ok = ok && wave == 0x45564157; // "WAVE"
    CHECK_OK("WAVE header");

    u32 fmt_id = read_u32();
    ok = ok && fmt_id == 0x20746D66; // "FMT"
    CHECK_OK("FMT header");

    u32 fmt_size = read_u32();
    ok = ok && fmt_size == 16;
    CHECK_OK("FMT size");

    u16 audio_format = read_u16();
    CHECK_OK("Audio format"); // incomplete read check
    ok = ok && (audio_format == WAVE_FORMAT_PCM || audio_format == WAVE_FORMAT_IEEE_FLOAT);
    CHECK_OK("Audio format PCM/Float"); // value check

    m_num_channels = read_u16();
    ok = ok && (m_num_channels == 1 || m_num_channels == 2);
    CHECK_OK("Channel count");

    m_sample_rate = read_u32();
    CHECK_OK("Sample rate");

    read_u32();
    CHECK_OK("Data rate");

    read_u16();
    CHECK_OK("Block size");

    u16 bits_per_sample = read_u16();
    CHECK_OK("Bits per sample"); // incomplete read check
    if (audio_format == WAVE_FORMAT_PCM) {
        ok = ok && (bits_per_sample == 8 || bits_per_sample == 16 || bits_per_sample == 24);
        CHECK_OK("Bits per sample (PCM)"); // value check

        // We only support 8-24 bit audio right now because other formats are uncommon
        if (bits_per_sample == 8) {
            m_sample_format = PcmSampleFormat::Uint8;
        } else if (bits_per_sample == 16) {
            m_sample_format = PcmSampleFormat::Int16;
        } else if (bits_per_sample == 24) {
            m_sample_format = PcmSampleFormat::Int24;
        }
    } else if (audio_format == WAVE_FORMAT_IEEE_FLOAT) {
        ok = ok && (bits_per_sample == 32 || bits_per_sample == 64);
        CHECK_OK("Bits per sample (Float)"); // value check

        // Again, only the common 32 and 64 bit
        if (bits_per_sample == 32) {
            m_sample_format = PcmSampleFormat::Float32;
        } else if (bits_per_sample == 64) {
            m_sample_format = PcmSampleFormat::Float64;
        }
    }

    dbgln_if(AWAVLOADER_DEBUG, "WAV format {} at {} bit, {} channels, rate {}Hz ",
        sample_format_name(m_sample_format), pcm_bits_per_sample(m_sample_format), m_num_channels, m_sample_rate);

    // Read chunks until we find DATA
    bool found_data = false;
    u32 data_sz = 0;
    u8 search_byte = 0;
    while (true) {
        search_byte = read_u8();
        CHECK_OK("Reading byte searching for data");
        if (search_byte != 0x64) //D
            continue;

        search_byte = read_u8();
        CHECK_OK("Reading next byte searching for data");
        if (search_byte != 0x61) //A
            continue;

        u16 search_remaining = read_u16();
        CHECK_OK("Reading remaining bytes searching for data");
        if (search_remaining != 0x6174) //TA
            continue;

        data_sz = read_u32();
        found_data = true;
        break;
    }

    ok = ok && found_data;
    CHECK_OK("Found no data chunk");

    ok = ok && data_sz < maximum_wav_size;
    CHECK_OK("Data was too large");

    int bytes_per_sample = (bits_per_sample / 8) * m_num_channels;
    m_total_samples = data_sz / bytes_per_sample;

    return true;
}

ResampleHelper::ResampleHelper(double source, double target)
    : m_ratio(source / target)
{
}

void ResampleHelper::process_sample(double sample_l, double sample_r)
{
    m_last_sample_l = sample_l;
    m_last_sample_r = sample_r;
    m_current_ratio += 1;
}

bool ResampleHelper::read_sample(double& next_l, double& next_r)
{
    if (m_current_ratio > 0) {
        m_current_ratio -= m_ratio;
        next_l = m_last_sample_l;
        next_r = m_last_sample_r;
        return true;
    }

    return false;
}

}
