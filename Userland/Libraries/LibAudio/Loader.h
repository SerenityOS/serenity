/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
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

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <LibAudio/Buffer.h>
#include <LibCore/File.h>

namespace Audio {

class LoaderPlugin {
public:
    virtual ~LoaderPlugin() { }

    virtual bool sniff() = 0;

    virtual bool has_error() { return false; }
    virtual const char* error_string() { return ""; }

    virtual RefPtr<Buffer> get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) = 0;

    virtual void reset() = 0;
    virtual void seek(const int position) = 0;

    virtual int loaded_samples() = 0;
    virtual int total_samples() = 0;
    virtual u32 sample_rate() = 0;
    virtual u16 num_channels() = 0;
    virtual u16 bits_per_sample() = 0;
    virtual RefPtr<Core::File> file() = 0;
};

class Loader : public RefCounted<Loader> {
public:
    static NonnullRefPtr<Loader> create(const StringView& path) { return adopt(*new Loader(path)); }
    static NonnullRefPtr<Loader> create(const ByteBuffer& buffer) { return adopt(*new Loader(buffer)); }

    bool has_error() const { return m_plugin ? m_plugin->has_error() : true; }
    const char* error_string() const { return m_plugin ? m_plugin->error_string() : "No loader plugin available"; }

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
    u16 bits_per_sample() const { return m_plugin ? m_plugin->bits_per_sample() : 0; }
    RefPtr<Core::File> file() const { return m_plugin ? m_plugin->file() : nullptr; }

private:
    Loader(const StringView& path);
    Loader(const ByteBuffer& buffer);

    mutable OwnPtr<LoaderPlugin> m_plugin;
};

}
