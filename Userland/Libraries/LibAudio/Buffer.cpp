/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Buffer.h"
#include <AK/Atomic.h>
#include <AK/Debug.h>
#include <AK/StdLibExtras.h>
#include <AK/String.h>

namespace Audio {

u16 pcm_bits_per_sample(PcmSampleFormat format)
{
    switch (format) {
    case Uint8:
        return 8;
    case Int16:
        return 16;
    case Int24:
        return 24;
    case Int32:
    case Float32:
        return 32;
    case Float64:
        return 64;
    default:
        VERIFY_NOT_REACHED();
    }
}

String sample_format_name(PcmSampleFormat format)
{
    bool is_float = format == Float32 || format == Float64;
    return String::formatted("PCM {}bit {}", pcm_bits_per_sample(format), is_float ? "Float" : "LE");
}

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

template<typename SampleType>
ResampleHelper<SampleType>::ResampleHelper(u32 source, u32 target)
    : m_source(source)
    , m_target(target)
{
    VERIFY(source > 0);
    VERIFY(target > 0);
}
template ResampleHelper<i32>::ResampleHelper(u32, u32);
template ResampleHelper<double>::ResampleHelper(u32, u32);

template<typename SampleType>
Vector<SampleType> ResampleHelper<SampleType>::resample(Vector<SampleType> to_resample)
{
    Vector<SampleType> resampled;
    resampled.ensure_capacity(to_resample.size() * ceil_div(m_source, m_target));
    for (auto sample : to_resample) {
        process_sample(sample, sample);

        while (read_sample(sample, sample))
            resampled.unchecked_append(sample);
    }

    return resampled;
}
template Vector<i32> ResampleHelper<i32>::resample(Vector<i32>);
template Vector<double> ResampleHelper<double>::resample(Vector<double>);

ErrorOr<NonnullRefPtr<Buffer>> resample_buffer(ResampleHelper<double>& resampler, Buffer const& to_resample)
{
    Vector<Sample> resampled;
    resampled.ensure_capacity(to_resample.sample_count() * ceil_div(resampler.source(), resampler.target()));
    for (size_t i = 0; i < static_cast<size_t>(to_resample.sample_count()); ++i) {
        auto sample = to_resample.samples()[i];
        resampler.process_sample(sample.left, sample.right);

        while (resampler.read_sample(sample.left, sample.right))
            resampled.append(sample);
    }

    return Buffer::create_with_samples(move(resampled));
}

template<typename SampleType>
void ResampleHelper<SampleType>::process_sample(SampleType sample_l, SampleType sample_r)
{
    m_last_sample_l = sample_l;
    m_last_sample_r = sample_r;
    m_current_ratio += m_target;
}
template void ResampleHelper<i32>::process_sample(i32, i32);
template void ResampleHelper<double>::process_sample(double, double);

template<typename SampleType>
bool ResampleHelper<SampleType>::read_sample(SampleType& next_l, SampleType& next_r)
{
    if (m_current_ratio >= m_source) {
        m_current_ratio -= m_source;
        next_l = m_last_sample_l;
        next_r = m_last_sample_r;
        return true;
    }

    return false;
}
template bool ResampleHelper<i32>::read_sample(i32&, i32&);
template bool ResampleHelper<double>::read_sample(double&, double&);

template<typename SampleType>
void ResampleHelper<SampleType>::reset()
{
    m_current_ratio = 0;
    m_last_sample_l = {};
    m_last_sample_r = {};
}

template void ResampleHelper<i32>::reset();
template void ResampleHelper<double>::reset();

}
