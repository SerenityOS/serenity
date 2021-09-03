/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/RefCounted.h>
#include <YAK/RefPtr.h>
#include <YAK/StringView.h>
#include <LibAudio/Buffer.h>
#include <LibCore/File.h>

namespace Audio {

static const String empty_string = "";
static String no_plugin_error = "No loader plugin available";

class LoaderPlugin {
public:
    virtual ~LoaderPlugin() { }

    virtual bool sniff() = 0;

    virtual bool has_error() { return false; }
    virtual const String& error_string() { return empty_string; }

    virtual RefPtr<Buffer> get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) = 0;

    virtual void reset() = 0;

    virtual void seek(const int sample_index) = 0;

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

    virtual PcmSampleFormat pcm_format() = 0;
    virtual RefPtr<Core::File> file() = 0;
};

class Loader : public RefCounted<Loader> {
public:
    static NonnullRefPtr<Loader> create(const StringView& path) { return adopt_ref(*new Loader(path)); }
    static NonnullRefPtr<Loader> create(const ByteBuffer& buffer) { return adopt_ref(*new Loader(buffer)); }

    bool has_error() const { return m_plugin ? m_plugin->has_error() : true; }
    const String& error_string() const { return m_plugin ? m_plugin->error_string() : no_plugin_error; }

    RefPtr<Buffer> get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) const { return m_plugin ? m_plugin->get_more_samples(max_bytes_to_read_from_input) : nullptr; }

    void reset() const
    {
        if (m_plugin)
            m_plugin->reset();
    }
    void seek(const int position) const
    {
        if (m_plugin)
            m_plugin->seek(position);
    }

    int loaded_samples() const { return m_plugin ? m_plugin->loaded_samples() : 0; }
    int total_samples() const { return m_plugin ? m_plugin->total_samples() : 0; }
    u32 sample_rate() const { return m_plugin ? m_plugin->sample_rate() : 0; }
    u16 num_channels() const { return m_plugin ? m_plugin->num_channels() : 0; }
    u16 bits_per_sample() const { return m_plugin ? pcm_bits_per_sample(m_plugin->pcm_format()) : 0; }
    RefPtr<Core::File> file() const { return m_plugin ? m_plugin->file() : nullptr; }

private:
    Loader(const StringView& path);
    Loader(const ByteBuffer& buffer);

    mutable OwnPtr<LoaderPlugin> m_plugin;
};

}
