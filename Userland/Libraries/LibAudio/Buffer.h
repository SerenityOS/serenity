/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/TypedTransfer.h"
#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <AK/kmalloc.h>
#include <LibAudio/Resampler.h>
#include <LibAudio/Sample.h>
#include <LibAudio/SampleFormats.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibCore/SharedCircularQueue.h>
#include <string.h>

namespace Audio {

static constexpr size_t AUDIO_BUFFERS_COUNT = 128;
// The audio buffer size is specifically chosen to be about 1/1000th of a second (1ms).
// This has the biggest impact on latency and performance.
// The currently chosen value was not put here with much thought and a better choice is surely possible.
static constexpr size_t AUDIO_BUFFER_SIZE = 50;
using AudioQueue = Core::SharedSingleProducerCircularQueue<Array<Sample, AUDIO_BUFFER_SIZE>, AUDIO_BUFFERS_COUNT>;

using namespace AK::Exponentials;

// A buffer of audio samples.
class LegacyBuffer : public RefCounted<LegacyBuffer> {
public:
    static ErrorOr<NonnullRefPtr<LegacyBuffer>> from_pcm_data(ReadonlyBytes data, int num_channels, PcmSampleFormat sample_format);
    static ErrorOr<NonnullRefPtr<LegacyBuffer>> from_pcm_stream(InputMemoryStream& stream, int num_channels, PcmSampleFormat sample_format, int num_samples);
    template<ArrayLike<Sample> ArrayT>
    static ErrorOr<NonnullRefPtr<LegacyBuffer>> create_with_samples(ArrayT&& samples)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) LegacyBuffer(move(samples)));
    }
    static ErrorOr<NonnullRefPtr<LegacyBuffer>> create_with_anonymous_buffer(Core::AnonymousBuffer buffer, i32 buffer_id, int sample_count)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) LegacyBuffer(move(buffer), buffer_id, sample_count));
    }
    static NonnullRefPtr<LegacyBuffer> create_empty()
    {
        // If we can't allocate an empty buffer, things are in a very bad state.
        return MUST(adopt_nonnull_ref_or_enomem(new (nothrow) LegacyBuffer));
    }

    Sample const* samples() const { return (Sample const*)data(); }

    ErrorOr<FixedArray<Sample>> to_sample_array() const
    {
        FixedArray<Sample> samples = TRY(FixedArray<Sample>::try_create(m_sample_count));
        AK::TypedTransfer<Sample>::copy(samples.data(), this->samples(), m_sample_count);
        return samples;
    }

    int sample_count() const { return m_sample_count; }
    void const* data() const { return m_buffer.data<void>(); }
    int size_in_bytes() const { return m_sample_count * (int)sizeof(Sample); }
    int id() const { return m_id; }
    Core::AnonymousBuffer const& anonymous_buffer() const { return m_buffer; }

private:
    template<ArrayLike<Sample> ArrayT>
    explicit LegacyBuffer(ArrayT&& samples)
        : m_buffer(Core::AnonymousBuffer::create_with_size(samples.size() * sizeof(Sample)).release_value())
        , m_id(allocate_id())
        , m_sample_count(samples.size())
    {
        memcpy(m_buffer.data<void>(), samples.data(), samples.size() * sizeof(Sample));
    }

    explicit LegacyBuffer(Core::AnonymousBuffer buffer, i32 buffer_id, int sample_count)
        : m_buffer(move(buffer))
        , m_id(buffer_id)
        , m_sample_count(sample_count)
    {
    }

    // Empty Buffer representation, to avoid tiny anonymous buffers in EOF states
    LegacyBuffer() = default;

    static i32 allocate_id();

    Core::AnonymousBuffer m_buffer;
    const i32 m_id { -1 };
    int const m_sample_count { 0 };
};

// This only works for double resamplers, and therefore cannot be part of the class
ErrorOr<NonnullRefPtr<LegacyBuffer>> resample_buffer(ResampleHelper<double>& resampler, LegacyBuffer const& to_resample);

}
