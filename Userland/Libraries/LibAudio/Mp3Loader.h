/*
 * Copyright (c) 2021, János Tóth <toth-janos@outlook.com>
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
#include <AK/MemoryStream.h>
#include <AK/NonnullPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Mp3Frame.h>
#include <LibAudio/Mp3Id3.h>
#include <LibCore/File.h>
#include <LibCore/IODeviceStreamReader.h>

using namespace Audio::Mp3;

namespace Audio {

class Buffer;

// Parses an MP3 file and produces an Audio::Buffer.
class Mp3LoaderPlugin : public LoaderPlugin {
    friend class Mp3::Id3;
    friend class Mp3::Frame;

public:
    Mp3LoaderPlugin(const StringView&);
    Mp3LoaderPlugin(const ByteBuffer&);

    virtual bool sniff() override;

    virtual bool has_error() override { return !m_error_string.is_empty(); }
    virtual const char* error_string() override { return m_error_string.characters(); }

    virtual RefPtr<Buffer> get_more_samples(size_t) override;

    virtual void reset() override;
    virtual void seek(const int) override;
    void seek_raw(size_t);

    virtual int loaded_samples() override { return m_loaded_samples; }
    virtual int total_samples() override { return m_samples.size(); }
    virtual u32 sample_rate() override { return m_sample_rate; }
    virtual u16 num_channels() override { return m_num_channels; }
    virtual u16 bits_per_sample() override { return m_bits_per_sample; }
    virtual RefPtr<Core::File> file() override { return m_file; }

    const Vector<ByteBuffer>& loaded_data() const { return m_loaded_data; };

private:
    bool parse_header();

    u8 read_byte(bool&);
    void read(Bytes, bool&);

    // FIXME: Find a place to clear the data if the stream is large or "infinite" (e.g. audio stream).
    // FIXME: Using Vector as a first-in-last-out container, use a proper "stack".
    void append_data(const ByteBuffer& data) { m_loaded_data.insert(0, data); };

    bool read_next_frame();

    void read_at_least(u32);
    void read_at_least(float);

    bool m_valid { false };
    RefPtr<Core::File> m_file;
    OwnPtr<InputMemoryStream> m_stream;

    String m_error_string;
    OwnPtr<Mp3::Id3> m_id3;
    OwnPtr<Mp3::Frame> m_current_frame;

    Vector<RefPtr<Buffer>> m_samples;
    Vector<Audio::Frame> m_loaded_frames;

    Vector<ByteBuffer> m_loaded_data;

    u32 m_sample_rate { 0 };
    u16 m_num_channels { 0 };
    u16 m_bits_per_sample { 0 };

    int m_loaded_samples { 0 };

    float m_raw_samples[2][2][576] = {};

    // NOTE: It's faster to use two instead of awlays re-clearing one.
    float m_temp_pcm[576] = {};
    float m_temp_pcm2[576] = {};

    float m_imdct_sample_block[36] = {};
    float m_imdct_temp_block[36] = {};

    float m_synth_s[32] = {};
    float m_synth_u[512] = {};
    float m_synth_w[512] = {};

    float m_prev_samples[2][32][18] = {};
    float m_fifo[2][1024] = {};
};

}
