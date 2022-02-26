/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Buffer.h"
#include <AK/Atomic.h>
#include <AK/Debug.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>

namespace Audio {

i32 Buffer::allocate_id()
{
    static Atomic<i32> next_id;
    return next_id++;
}

template<typename SampleReader>
static void read_samples_from_stream(InputMemoryStream& stream, SampleReader read_sample, Vector<Sample>& samples, int num_channels)
{
    double left_channel_sample = 0;
    double right_channel_sample = 0;

    switch (num_channels) {
    case 1:
        for (;;) {
            left_channel_sample = read_sample(stream);
            samples.append(Sample(left_channel_sample));

            if (stream.handle_any_error()) {
                break;
            }
        }
        break;
    case 2:
        for (;;) {
            left_channel_sample = read_sample(stream);
            right_channel_sample = read_sample(stream);
            samples.append(Sample(left_channel_sample, right_channel_sample));

            if (stream.handle_any_error()) {
                break;
            }
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

static double read_float_sample_64(InputMemoryStream& stream)
{
    LittleEndian<double> sample;
    stream >> sample;
    return double(sample);
}

static double read_float_sample_32(InputMemoryStream& stream)
{
    LittleEndian<float> sample;
    stream >> sample;
    return double(sample);
}

static double read_norm_sample_24(InputMemoryStream& stream)
{
    u8 byte = 0;
    stream >> byte;
    u32 sample1 = byte;
    stream >> byte;
    u32 sample2 = byte;
    stream >> byte;
    u32 sample3 = byte;

    i32 value = 0;
    value = sample1 << 8;
    value |= (sample2 << 16);
    value |= (sample3 << 24);
    return double(value) / NumericLimits<i32>::max();
}

static double read_norm_sample_16(InputMemoryStream& stream)
{
    LittleEndian<i16> sample;
    stream >> sample;
    return double(sample) / NumericLimits<i16>::max();
}

static double read_norm_sample_8(InputMemoryStream& stream)
{
    u8 sample = 0;
    stream >> sample;
    return double(sample) / NumericLimits<u8>::max();
}

ErrorOr<NonnullRefPtr<Buffer>> Buffer::from_pcm_data(ReadonlyBytes data, int num_channels, PcmSampleFormat sample_format)
{
    InputMemoryStream stream { data };
    return from_pcm_stream(stream, num_channels, sample_format, data.size() / (pcm_bits_per_sample(sample_format) / 8));
}

ErrorOr<NonnullRefPtr<Buffer>> Buffer::from_pcm_stream(InputMemoryStream& stream, int num_channels, PcmSampleFormat sample_format, int num_samples)
{
    Vector<Sample> fdata;
    fdata.ensure_capacity(num_samples);

    switch (sample_format) {
    case PcmSampleFormat::Uint8:
        read_samples_from_stream(stream, read_norm_sample_8, fdata, num_channels);
        break;
    case PcmSampleFormat::Int16:
        read_samples_from_stream(stream, read_norm_sample_16, fdata, num_channels);
        break;
    case PcmSampleFormat::Int24:
        read_samples_from_stream(stream, read_norm_sample_24, fdata, num_channels);
        break;
    case PcmSampleFormat::Float32:
        read_samples_from_stream(stream, read_float_sample_32, fdata, num_channels);
        break;
    case PcmSampleFormat::Float64:
        read_samples_from_stream(stream, read_float_sample_64, fdata, num_channels);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // We should handle this in a better way above, but for now --
    // just make sure we're good. Worst case we just write some 0s where they
    // don't belong.
    VERIFY(!stream.handle_any_error());

    return Buffer::create_with_samples(move(fdata));
}

}
