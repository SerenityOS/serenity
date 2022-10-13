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
#include <AK/StringView.h>
#include <AK/Try.h>
#include <LibAudio/LoaderError.h>
#include <LibAudio/Sample.h>
#include <LibAudio/SampleFormats.h>
#include <LibCore/Stream.h>

namespace Audio {

static constexpr StringView no_plugin_error = "No loader plugin available"sv;

using LoaderSamples = Result<FixedArray<Sample>, LoaderError>;
using MaybeLoaderError = Result<void, LoaderError>;

class LoaderPlugin {
public:
    explicit LoaderPlugin(StringView path);
    explicit LoaderPlugin(Bytes buffer);
    virtual ~LoaderPlugin() = default;

    virtual MaybeLoaderError initialize() = 0;

    virtual LoaderSamples get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) = 0;

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
    virtual String format_name() = 0;
    virtual PcmSampleFormat pcm_format() = 0;

protected:
    StringView m_path;
    OwnPtr<Core::Stream::SeekableStream> m_stream;
    // The constructor might set this so that we can initialize the data stream later.
    Optional<Bytes> m_backing_memory;
};

class Loader : public RefCounted<Loader> {
public:
    static Result<NonnullRefPtr<Loader>, LoaderError> create(StringView path) { return adopt_ref(*new Loader(TRY(try_create(path)))); }
    static Result<NonnullRefPtr<Loader>, LoaderError> create(Bytes& buffer) { return adopt_ref(*new Loader(TRY(try_create(buffer)))); }

    LoaderSamples get_more_samples(size_t max_samples_to_read_from_input = 128 * KiB) const { return m_plugin->get_more_samples(max_samples_to_read_from_input); }

    MaybeLoaderError reset() const { return m_plugin->reset(); }
    MaybeLoaderError seek(int const position) const { return m_plugin->seek(position); }

    int loaded_samples() const { return m_plugin->loaded_samples(); }
    int total_samples() const { return m_plugin->total_samples(); }
    u32 sample_rate() const { return m_plugin->sample_rate(); }
    u16 num_channels() const { return m_plugin->num_channels(); }
    String format_name() const { return m_plugin->format_name(); }
    u16 bits_per_sample() const { return pcm_bits_per_sample(m_plugin->pcm_format()); }

private:
    static Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> try_create(StringView path);
    static Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> try_create(Bytes& buffer);

    explicit Loader(NonnullOwnPtr<LoaderPlugin>);

    mutable NonnullOwnPtr<LoaderPlugin> m_plugin;
};

}
