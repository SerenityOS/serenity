/*
 * Copyright (c) 2018-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Result.h>
#include <AK/Span.h>
#include <AK/Stream.h>
#include <AK/StringView.h>
#include <AK/Try.h>
#include <LibAudio/GenericTypes.h>
#include <LibAudio/LoaderError.h>
#include <LibAudio/Sample.h>
#include <LibAudio/SampleFormats.h>

namespace Audio {

static constexpr StringView no_plugin_error = "No loader plugin available"sv;

// Experimentally determined to be a decent buffer size on i686:
// 4K (the default) is slightly worse, and 64K is much worse.
// At sufficiently large buffer sizes, the advantage of infrequent read() calls is outweighed by the memmove() overhead.
// There was no intensive fine-tuning done to determine this value, so improvements may definitely be possible.
constexpr size_t const loader_buffer_size = 8 * KiB;

using LoaderSamples = ErrorOr<FixedArray<Sample>, LoaderError>;
using MaybeLoaderError = ErrorOr<void, LoaderError>;

class LoaderPlugin {
public:
    explicit LoaderPlugin(NonnullOwnPtr<SeekableStream> stream);
    virtual ~LoaderPlugin() = default;

    // Load as many audio chunks as necessary to get up to the required samples.
    // A chunk can be anything that is convenient for the plugin to load in one go without requiring to move samples around different buffers.
    // For example: A FLAC, MP3 or QOA frame.
    // The chunks are returned in a vector, so the loader can simply add chunks until the requested sample amount is reached.
    // The sample count MAY be surpassed, but only as little as possible. It CAN be undershot when the end of the stream is reached.
    // If the loader has no chunking limitations (e.g. WAV), it may return a single exact-sized chunk.
    virtual ErrorOr<Vector<FixedArray<Sample>>, LoaderError> load_chunks(size_t samples_to_read_from_input) = 0;

    virtual MaybeLoaderError reset() = 0;

    virtual MaybeLoaderError seek(int const sample_index) = 0;

    // total_samples() and loaded_samples() should be independent
    // of the number of channels.
    //
    // For example, with a three-second-long, stereo, 44.1KHz audio file:
    //    num_channels() should return 2
    //    sample_rate() should return 44100 (each channel is sampled at this rate)
    //    total_samples() should return 132300 (sample_rate * three seconds)
    virtual int loaded_samples() = 0;
    virtual int total_samples() = 0;
    virtual u32 sample_rate() = 0;
    virtual u16 num_channels() = 0;

    // Human-readable name of the file format, of the form <full abbreviation> (.<ending>)
    virtual DeprecatedString format_name() = 0;
    virtual PcmSampleFormat pcm_format() = 0;

    Vector<PictureData> const& pictures() const { return m_pictures; };

protected:
    NonnullOwnPtr<SeekableStream> m_stream;

    Vector<PictureData> m_pictures;
};

class Loader : public RefCounted<Loader> {
public:
    static Result<NonnullRefPtr<Loader>, LoaderError> create(StringView path) { return adopt_ref(*new Loader(TRY(create_plugin(path)))); }
    static Result<NonnullRefPtr<Loader>, LoaderError> create(Bytes buffer) { return adopt_ref(*new Loader(TRY(create_plugin(buffer)))); }

    // Will only read less samples if we're at the end of the stream.
    LoaderSamples get_more_samples(size_t samples_to_read_from_input = 128 * KiB);

    MaybeLoaderError reset() const { return m_plugin->reset(); }
    MaybeLoaderError seek(int const position) const
    {
        m_buffer.clear_with_capacity();
        return m_plugin->seek(position);
    }

    int loaded_samples() const { return m_plugin->loaded_samples(); }
    int total_samples() const { return m_plugin->total_samples(); }
    u32 sample_rate() const { return m_plugin->sample_rate(); }
    u16 num_channels() const { return m_plugin->num_channels(); }
    DeprecatedString format_name() const { return m_plugin->format_name(); }
    u16 bits_per_sample() const { return pcm_bits_per_sample(m_plugin->pcm_format()); }
    Vector<PictureData> const& pictures() const { return m_plugin->pictures(); };

private:
    static Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> create_plugin(StringView path);
    static Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> create_plugin(Bytes buffer);

    explicit Loader(NonnullOwnPtr<LoaderPlugin>);

    mutable NonnullOwnPtr<LoaderPlugin> m_plugin;
    mutable Vector<Sample, loader_buffer_size> m_buffer;
};

}
