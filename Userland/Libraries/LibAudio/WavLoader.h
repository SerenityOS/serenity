/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibAudio/Loader.h>
#include <LibCore/File.h>
#include <LibCore/Stream.h>

namespace Audio {

// constants for handling the WAV header data
static constexpr unsigned const WAVE_FORMAT_PCM = 0x0001;        // PCM
static constexpr unsigned const WAVE_FORMAT_IEEE_FLOAT = 0x0003; // IEEE float
static constexpr unsigned const WAVE_FORMAT_ALAW = 0x0006;       // 8-bit ITU-T G.711 A-law
static constexpr unsigned const WAVE_FORMAT_MULAW = 0x0007;      // 8-bit ITU-T G.711 µ-law
static constexpr unsigned const WAVE_FORMAT_EXTENSIBLE = 0xFFFE; // Determined by SubFormat

// Parses and reads audio data from a WAV file.
class WavLoaderPlugin : public LoaderPlugin {
public:
    explicit WavLoaderPlugin(StringView path);
    explicit WavLoaderPlugin(Bytes const& buffer);

    virtual MaybeLoaderError initialize() override;

    virtual LoaderSamples get_more_samples(size_t max_samples_to_read_from_input = 128 * KiB) override;

    virtual MaybeLoaderError reset() override { return seek(0); }

    // sample_index 0 is the start of the raw audio sample data
    // within the file/stream.
    virtual MaybeLoaderError seek(int sample_index) override;

    virtual int loaded_samples() override { return static_cast<int>(m_loaded_samples); }
    virtual int total_samples() override { return static_cast<int>(m_total_samples); }
    virtual u32 sample_rate() override { return m_sample_rate; }
    virtual u16 num_channels() override { return m_num_channels; }
    virtual String format_name() override { return "RIFF WAVE (.wav)"; }
    virtual PcmSampleFormat pcm_format() override { return m_sample_format; }

private:
    MaybeLoaderError parse_header();

    LoaderSamples samples_from_pcm_data(Bytes const& data, size_t samples_to_read) const;
    template<typename SampleReader>
    MaybeLoaderError read_samples_from_stream(Core::Stream::Stream& stream, SampleReader read_sample, FixedArray<Sample>& samples) const;

    StringView m_path;
    OwnPtr<Core::Stream::SeekableStream> m_stream;
    // The constructor might set this so that we can initialize the data stream later.
    Optional<Bytes const&> m_backing_memory;

    u32 m_sample_rate { 0 };
    u16 m_num_channels { 0 };
    PcmSampleFormat m_sample_format;
    size_t m_byte_offset_of_data_samples { 0 };

    size_t m_loaded_samples { 0 };
    size_t m_total_samples { 0 };
};

}
