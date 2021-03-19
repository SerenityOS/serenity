/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/AnonymousBuffer.h>
#include <string.h>

namespace Audio {

// A single sample in an audio buffer.
// Values are floating point, and should range from -1.0 to +1.0
struct Frame {
    Frame()
        : left(0)
        , right(0)
    {
    }

    // For mono
    Frame(double left)
        : left(left)
        , right(left)
    {
    }

    // For stereo
    Frame(double left, double right)
        : left(left)
        , right(right)
    {
    }

    void clip()
    {
        if (left > 1)
            left = 1;
        else if (left < -1)
            left = -1;

        if (right > 1)
            right = 1;
        else if (right < -1)
            right = -1;
    }

    void scale(int percent)
    {
        double pct = (double)percent / 100.0;
        left *= pct;
        right *= pct;
    }

    Frame& operator+=(const Frame& other)
    {
        left += other.left;
        right += other.right;
        return *this;
    }

    double left;
    double right;
};

// Small helper to resample from one playback rate to another
// This isn't really "smart", in that we just insert (or drop) samples.
// Should do better...
class ResampleHelper {
public:
    ResampleHelper(double source, double target);

    void process_sample(double sample_l, double sample_r);
    bool read_sample(double& next_l, double& next_r);

private:
    const double m_ratio;
    double m_current_ratio { 0 };
    double m_last_sample_l { 0 };
    double m_last_sample_r { 0 };
};

// A buffer of audio samples, normalized to 44100hz.
class Buffer : public RefCounted<Buffer> {
public:
    static RefPtr<Buffer> from_pcm_data(ReadonlyBytes data, ResampleHelper& resampler, int num_channels, int bits_per_sample);
    static RefPtr<Buffer> from_pcm_stream(InputMemoryStream& stream, ResampleHelper& resampler, int num_channels, int bits_per_sample, int num_samples);
    static NonnullRefPtr<Buffer> create_with_samples(Vector<Frame>&& samples)
    {
        return adopt(*new Buffer(move(samples)));
    }
    static NonnullRefPtr<Buffer> create_with_anonymous_buffer(Core::AnonymousBuffer buffer, i32 buffer_id, int sample_count)
    {
        return adopt(*new Buffer(move(buffer), buffer_id, sample_count));
    }

    const Frame* samples() const { return (const Frame*)data(); }
    int sample_count() const { return m_sample_count; }
    const void* data() const { return m_buffer.data<void>(); }
    int size_in_bytes() const { return m_sample_count * (int)sizeof(Frame); }
    int id() const { return m_id; }
    const Core::AnonymousBuffer& anonymous_buffer() const { return m_buffer; }

private:
    explicit Buffer(const Vector<Frame> samples)
        : m_buffer(Core::AnonymousBuffer::create_with_size(samples.size() * sizeof(Frame)))
        , m_id(allocate_id())
        , m_sample_count(samples.size())
    {
        memcpy(m_buffer.data<void>(), samples.data(), samples.size() * sizeof(Frame));
    }

    explicit Buffer(Core::AnonymousBuffer buffer, i32 buffer_id, int sample_count)
        : m_buffer(move(buffer))
        , m_id(buffer_id)
        , m_sample_count(sample_count)
    {
    }

    static i32 allocate_id();

    Core::AnonymousBuffer m_buffer;
    const i32 m_id;
    const int m_sample_count;
};

}
