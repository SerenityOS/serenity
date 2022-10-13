/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WavLoader.h"
#include "LoaderError.h"
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/FixedArray.h>
#include <AK/NumericLimits.h>
#include <AK/Try.h>
#include <LibCore/MemoryStream.h>

namespace Audio {

static constexpr size_t const maximum_wav_size = 1 * GiB; // FIXME: is there a more appropriate size limit?

WavLoaderPlugin::WavLoaderPlugin(StringView path)
    : LoaderPlugin(path)
{
}

MaybeLoaderError WavLoaderPlugin::initialize()
{
    LOADER_TRY(LoaderPlugin::initialize());

    TRY(parse_header());
    return {};
}

WavLoaderPlugin::WavLoaderPlugin(Bytes buffer)
    : LoaderPlugin(buffer)
{
}

template<typename SampleReader>
MaybeLoaderError WavLoaderPlugin::read_samples_from_stream(Core::Stream::Stream& stream, SampleReader read_sample, FixedArray<Sample>& samples) const
{
    switch (m_num_channels) {
    case 1:
        for (auto& sample : samples)
            sample = Sample(LOADER_TRY(read_sample(stream)));
        break;
    case 2:
        for (auto& sample : samples) {
            auto left_channel_sample = LOADER_TRY(read_sample(stream));
            auto right_channel_sample = LOADER_TRY(read_sample(stream));
            sample = Sample(left_channel_sample, right_channel_sample);
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return {};
}

// There's no i24 type + we need to do the endianness conversion manually anyways.
static ErrorOr<double> read_sample_int24(Core::Stream::Stream& stream)
{
    u8 byte = 0;
    TRY(stream.read(Bytes { &byte, 1 }));
    i32 sample1 = byte;
    TRY(stream.read(Bytes { &byte, 1 }));
    i32 sample2 = byte;
    TRY(stream.read(Bytes { &byte, 1 }));
    i32 sample3 = byte;

    i32 value = 0;
    value = sample1;
    value |= sample2 << 8;
    value |= sample3 << 16;
    // Sign extend the value, as it can currently not have the correct sign.
    value = (value << 8) >> 8;
    // Range of value is now -2^23 to 2^23-1 and we can rescale normally.
    return static_cast<double>(value) / static_cast<double>((1 << 23) - 1);
}

template<typename T>
static ErrorOr<double> read_sample(Core::Stream::Stream& stream)
{
    T sample { 0 };
    TRY(stream.read(Bytes { &sample, sizeof(T) }));
    // Remap integer samples to normalized floating-point range of -1 to 1.
    if constexpr (IsIntegral<T>) {
        if constexpr (NumericLimits<T>::is_signed()) {
            // Signed integer samples are centered around zero, so this division is enough.
            return static_cast<double>(AK::convert_between_host_and_little_endian(sample)) / static_cast<double>(NumericLimits<T>::max());
        } else {
            // Unsigned integer samples, on the other hand, need to be shifted to center them around zero.
            // The first division therefore remaps to the range 0 to 2.
            return static_cast<double>(AK::convert_between_host_and_little_endian(sample)) / (static_cast<double>(NumericLimits<T>::max()) / 2.0) - 1.0;
        }
    } else {
        return static_cast<double>(AK::convert_between_host_and_little_endian(sample));
    }
}

LoaderSamples WavLoaderPlugin::samples_from_pcm_data(Bytes const& data, size_t samples_to_read) const
{
    FixedArray<Sample> samples = LOADER_TRY(FixedArray<Sample>::try_create(samples_to_read));
    auto stream = LOADER_TRY(Core::Stream::MemoryStream::construct(move(data)));

    switch (m_sample_format) {
    case PcmSampleFormat::Uint8:
        TRY(read_samples_from_stream(*stream, read_sample<u8>, samples));
        break;
    case PcmSampleFormat::Int16:
        TRY(read_samples_from_stream(*stream, read_sample<i16>, samples));
        break;
    case PcmSampleFormat::Int24:
        TRY(read_samples_from_stream(*stream, read_sample_int24, samples));
        break;
    case PcmSampleFormat::Float32:
        TRY(read_samples_from_stream(*stream, read_sample<float>, samples));
        break;
    case PcmSampleFormat::Float64:
        TRY(read_samples_from_stream(*stream, read_sample<double>, samples));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return samples;
}

LoaderSamples WavLoaderPlugin::get_more_samples(size_t max_samples_to_read_from_input)
{
    if (!m_stream)
        return LoaderError { LoaderError::Category::Internal, static_cast<size_t>(m_loaded_samples), "No stream; initialization failed" };

    auto remaining_samples = m_total_samples - m_loaded_samples;
    if (remaining_samples <= 0)
        return FixedArray<Sample> {};

    // One "sample" contains data from all channels.
    // In the Wave spec, this is also called a block.
    size_t bytes_per_sample
        = m_num_channels * pcm_bits_per_sample(m_sample_format) / 8;

    // Might truncate if not evenly divisible by the sample size
    auto max_samples_to_read = max_samples_to_read_from_input / bytes_per_sample;
    auto samples_to_read = min(max_samples_to_read, remaining_samples);
    auto bytes_to_read = samples_to_read * bytes_per_sample;

    dbgln_if(AWAVLOADER_DEBUG, "Read {} bytes WAV with num_channels {} sample rate {}, "
                               "bits per sample {}, sample format {}",
        bytes_to_read, m_num_channels, m_sample_rate,
        pcm_bits_per_sample(m_sample_format), sample_format_name(m_sample_format));

    auto sample_data = LOADER_TRY(ByteBuffer::create_zeroed(bytes_to_read));
    LOADER_TRY(m_stream->read(sample_data.bytes()));

    // m_loaded_samples should contain the amount of actually loaded samples
    m_loaded_samples += samples_to_read;
    return samples_from_pcm_data(sample_data.bytes(), samples_to_read);
}

MaybeLoaderError WavLoaderPlugin::seek(int sample_index)
{
    dbgln_if(AWAVLOADER_DEBUG, "seek sample_index {}", sample_index);
    if (sample_index < 0 || sample_index >= static_cast<int>(m_total_samples))
        return LoaderError { LoaderError::Category::Internal, m_loaded_samples, "Seek outside the sample range" };

    size_t sample_offset = m_byte_offset_of_data_samples + static_cast<size_t>(sample_index * m_num_channels * (pcm_bits_per_sample(m_sample_format) / 8));

    LOADER_TRY(m_stream->seek(sample_offset, Core::Stream::SeekMode::SetPosition));

    m_loaded_samples = sample_index;
    return {};
}

// Specification reference: http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html
MaybeLoaderError WavLoaderPlugin::parse_header()
{
    if (!m_stream)
        return LoaderError { LoaderError::Category::Internal, 0, "No stream" };

    bool ok = true;
    size_t bytes_read = 0;

    auto read_u8 = [&]() -> ErrorOr<u8, LoaderError> {
        u8 value;
        LOADER_TRY(m_stream->read(Bytes { &value, 1 }));
        bytes_read += 1;
        return value;
    };

    auto read_u16 = [&]() -> ErrorOr<u16, LoaderError> {
        u16 value;
        LOADER_TRY(m_stream->read(Bytes { &value, 2 }));
        bytes_read += 2;
        return value;
    };

    auto read_u32 = [&]() -> ErrorOr<u32, LoaderError> {
        u32 value;
        LOADER_TRY(m_stream->read(Bytes { &value, 4 }));
        bytes_read += 4;
        return value;
    };

#define CHECK_OK(category, msg)                                                            \
    do {                                                                                   \
        if (!ok)                                                                           \
            return LoaderError { category, String::formatted("Parsing failed: {}", msg) }; \
    } while (0)

    u32 riff = TRY(read_u32());
    ok = ok && riff == 0x46464952; // "RIFF"
    CHECK_OK(LoaderError::Category::Format, "RIFF header");

    u32 sz = TRY(read_u32());
    ok = ok && sz < maximum_wav_size;
    CHECK_OK(LoaderError::Category::Format, "File size");

    u32 wave = TRY(read_u32());
    ok = ok && wave == 0x45564157; // "WAVE"
    CHECK_OK(LoaderError::Category::Format, "WAVE header");

    u32 fmt_id = TRY(read_u32());
    ok = ok && fmt_id == 0x20746D66; // "fmt "
    CHECK_OK(LoaderError::Category::Format, "FMT header");

    u32 fmt_size = TRY(read_u32());
    ok = ok && (fmt_size == 16 || fmt_size == 18 || fmt_size == 40);
    CHECK_OK(LoaderError::Category::Format, "FMT size");

    u16 audio_format = TRY(read_u16());
    CHECK_OK(LoaderError::Category::Format, "Audio format"); // incomplete read check
    ok = ok && (audio_format == WAVE_FORMAT_PCM || audio_format == WAVE_FORMAT_IEEE_FLOAT || audio_format == WAVE_FORMAT_EXTENSIBLE);
    CHECK_OK(LoaderError::Category::Unimplemented, "Audio format PCM/Float"); // value check

    m_num_channels = TRY(read_u16());
    ok = ok && (m_num_channels == 1 || m_num_channels == 2);
    CHECK_OK(LoaderError::Category::Unimplemented, "Channel count");

    m_sample_rate = TRY(read_u32());
    CHECK_OK(LoaderError::Category::IO, "Sample rate");

    TRY(read_u32());
    CHECK_OK(LoaderError::Category::IO, "Data rate");

    u16 block_size_bytes = TRY(read_u16());
    CHECK_OK(LoaderError::Category::IO, "Block size");

    u16 bits_per_sample = TRY(read_u16());
    CHECK_OK(LoaderError::Category::IO, "Bits per sample");

    if (audio_format == WAVE_FORMAT_EXTENSIBLE) {
        ok = ok && (fmt_size == 40);
        CHECK_OK(LoaderError::Category::Format, "Extensible fmt size"); // value check

        // Discard everything until the GUID.
        // We've already read 16 bytes from the stream. The GUID starts in another 8 bytes.
        TRY(read_u32());
        TRY(read_u32());
        CHECK_OK(LoaderError::Category::IO, "Discard until GUID");

        // Get the underlying audio format from the first two bytes of GUID
        u16 guid_subformat = TRY(read_u16());
        ok = ok && (guid_subformat == WAVE_FORMAT_PCM || guid_subformat == WAVE_FORMAT_IEEE_FLOAT);
        CHECK_OK(LoaderError::Category::Unimplemented, "GUID SubFormat");

        audio_format = guid_subformat;
    }

    if (audio_format == WAVE_FORMAT_PCM) {
        ok = ok && (bits_per_sample == 8 || bits_per_sample == 16 || bits_per_sample == 24);
        CHECK_OK(LoaderError::Category::Unimplemented, "Bits per sample (PCM)"); // value check

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
        CHECK_OK(LoaderError::Category::Unimplemented, "Bits per sample (Float)"); // value check

        // Again, only the common 32 and 64 bit
        if (bits_per_sample == 32) {
            m_sample_format = PcmSampleFormat::Float32;
        } else if (bits_per_sample == 64) {
            m_sample_format = PcmSampleFormat::Float64;
        }
    }

    ok = ok && (block_size_bytes == (m_num_channels * (bits_per_sample / 8)));
    CHECK_OK(LoaderError::Category::Format, "Block size sanity check");

    dbgln_if(AWAVLOADER_DEBUG, "WAV format {} at {} bit, {} channels, rate {}Hz ",
        sample_format_name(m_sample_format), pcm_bits_per_sample(m_sample_format), m_num_channels, m_sample_rate);

    // Read chunks until we find DATA
    bool found_data = false;
    u32 data_size = 0;
    u8 search_byte = 0;
    while (true) {
        search_byte = TRY(read_u8());
        CHECK_OK(LoaderError::Category::IO, "Reading byte searching for data");
        if (search_byte != 0x64) // D
            continue;

        search_byte = TRY(read_u8());
        CHECK_OK(LoaderError::Category::IO, "Reading next byte searching for data");
        if (search_byte != 0x61) // A
            continue;

        u16 search_remaining = TRY(read_u16());
        CHECK_OK(LoaderError::Category::IO, "Reading remaining bytes searching for data");
        if (search_remaining != 0x6174) // TA
            continue;

        data_size = TRY(read_u32());
        found_data = true;
        break;
    }

    ok = ok && found_data;
    CHECK_OK(LoaderError::Category::Format, "Found no data chunk");

    ok = ok && data_size < maximum_wav_size;
    CHECK_OK(LoaderError::Category::Format, "Data was too large");

    m_total_samples = data_size / block_size_bytes;

    dbgln_if(AWAVLOADER_DEBUG, "WAV data size {}, bytes per sample {}, total samples {}",
        data_size,
        block_size_bytes,
        m_total_samples);

    m_byte_offset_of_data_samples = bytes_read;
    return {};
}
}
