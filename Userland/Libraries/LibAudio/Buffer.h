/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <AK/kmalloc.h>
#include <LibAudio/Sample.h>
#include <LibCore/AnonymousBuffer.h>
#include <string.h>

namespace Audio {
using namespace AK::Exponentials;

// Supported PCM sample formats.
enum PcmSampleFormat : u8 {
    Uint8,
    Int16,
    Int24,
    Int32,
    Float32,
    Float64,
};

// Most of the read code only cares about how many bits to read or write
u16 pcm_bits_per_sample(PcmSampleFormat format);
String sample_format_name(PcmSampleFormat format);

// Small helper to resample from one playback rate to another
// This isn't really "smart", in that we just insert (or drop) samples.
// Should do better...
template<typename SampleType>
class ResampleHelper {
public:
    ResampleHelper(u32 source, u32 target);

    // To be used as follows:
    // while the resampler doesn't need a new sample, read_sample(current) and store the resulting samples.
    // as long as the resampler needs a new sample, process_sample(current)

    // Stores a new sample
    void process_sample(SampleType sample_l, SampleType sample_r);
    // Assigns the given sample to its correct value and returns false if there is a new sample required
    bool read_sample(SampleType& next_l, SampleType& next_r);
    Vector<SampleType> resample(Vector<SampleType> to_resample);

    void reset();

    u32 source() const { return m_source; }
    u32 target() const { return m_target; }

private:
    const u32 m_source;
    const u32 m_target;
    u32 m_current_ratio { 0 };
    SampleType m_last_sample_l;
    SampleType m_last_sample_r;
};

// A buffer of audio samples.
class Buffer : public RefCounted<Buffer> {
public:
    static ErrorOr<NonnullRefPtr<Buffer>> from_pcm_data(ReadonlyBytes data, int num_channels, PcmSampleFormat sample_format);
    static ErrorOr<NonnullRefPtr<Buffer>> from_pcm_stream(InputMemoryStream& stream, int num_channels, PcmSampleFormat sample_format, int num_samples);
    static ErrorOr<NonnullRefPtr<Buffer>> create_with_samples(Vector<Sample>&& samples)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) Buffer(move(samples)));
    }
    static ErrorOr<NonnullRefPtr<Buffer>> create_with_anonymous_buffer(Core::AnonymousBuffer buffer, i32 buffer_id, int sample_count)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) Buffer(move(buffer), buffer_id, sample_count));
    }
    static NonnullRefPtr<Buffer> create_empty()
    {
        // If we can't allocate an empty buffer, things are in a very bad state.
        return MUST(adopt_nonnull_ref_or_enomem(new (nothrow) Buffer({})));
    }

    const Sample* samples() const { return (const Sample*)data(); }
    int sample_count() const { return m_sample_count; }
    const void* data() const { return m_buffer.data<void>(); }
    int size_in_bytes() const { return m_sample_count * (int)sizeof(Sample); }
    int id() const { return m_id; }
    const Core::AnonymousBuffer& anonymous_buffer() const { return m_buffer; }

private:
    explicit Buffer(const Vector<Sample> samples)
        // FIXME: AnonymousBuffers can't be empty, so even for empty buffers we create a buffer of size 1 here,
        //        although the sample count is set to 0 to mark this.
        : m_buffer(Core::AnonymousBuffer::create_with_size(max(samples.size(), 1) * sizeof(Sample)).release_value())
        , m_id(allocate_id())
        , m_sample_count(samples.size())
    {
        memcpy(m_buffer.data<void>(), samples.data(), samples.size() * sizeof(Sample));
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

// This only works for double resamplers, and therefore cannot be part of the class
ErrorOr<NonnullRefPtr<Buffer>> resample_buffer(ResampleHelper<double>& resampler, Buffer const& to_resample);

}
