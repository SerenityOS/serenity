/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/MemoryStream.h>
#include <AK/String.h>
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

// Supported PCM sample formats.
enum PcmSampleFormat : u8 {
    Uint8,
    Int16,
    Int24,
    Float32,
    Float64,
};

// Most of the read code only cares about how many bits to read or write
u16 pcm_bits_per_sample(PcmSampleFormat format);
String sample_format_name(PcmSampleFormat format);

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
    static RefPtr<Buffer> from_pcm_data(ReadonlyBytes data, ResampleHelper& resampler, int num_channels, PcmSampleFormat sample_format);
    static RefPtr<Buffer> from_pcm_stream(InputMemoryStream& stream, ResampleHelper& resampler, int num_channels, PcmSampleFormat sample_format, int num_samples);
    static NonnullRefPtr<Buffer> create_with_samples(Vector<Frame>&& samples)
    {
        return adopt_ref(*new Buffer(move(samples)));
    }
    static NonnullRefPtr<Buffer> create_with_anonymous_buffer(Core::AnonymousBuffer buffer, i32 buffer_id, int sample_count)
    {
        return adopt_ref(*new Buffer(move(buffer), buffer_id, sample_count));
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
