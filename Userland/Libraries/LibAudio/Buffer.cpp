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

#include <AK/Atomic.h>
#include <LibAudio/Buffer.h>

namespace Audio {

i32 Buffer::allocate_id()
{
    static Atomic<i32> next_id;
    return next_id++;
}

template<typename SampleReader>
static void read_samples_from_stream(InputMemoryStream& stream, SampleReader read_sample, Vector<Frame>& samples, ResampleHelper& resampler, int num_channels)
{
    double norm_l = 0;
    double norm_r = 0;

    switch (num_channels) {
    case 1:
        for (;;) {
            while (resampler.read_sample(norm_l, norm_r)) {
                samples.append(Frame(norm_l));
            }
            norm_l = read_sample(stream);

            if (stream.handle_any_error()) {
                break;
            }
            resampler.process_sample(norm_l, norm_r);
        }
        break;
    case 2:
        for (;;) {
            while (resampler.read_sample(norm_l, norm_r)) {
                samples.append(Frame(norm_l, norm_r));
            }
            norm_l = read_sample(stream);
            norm_r = read_sample(stream);

            if (stream.handle_any_error()) {
                break;
            }
            resampler.process_sample(norm_l, norm_r);
        }
        break;
    default:
        VERIFY_NOT_REACHED();
    }
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

RefPtr<Buffer> Buffer::from_pcm_data(ReadonlyBytes data, ResampleHelper& resampler, int num_channels, int bits_per_sample)
{
    InputMemoryStream stream { data };
    return from_pcm_stream(stream, resampler, num_channels, bits_per_sample, data.size() / (bits_per_sample / 8));
}

RefPtr<Buffer> Buffer::from_pcm_stream(InputMemoryStream& stream, ResampleHelper& resampler, int num_channels, int bits_per_sample, int num_samples)
{
    Vector<Frame> fdata;
    fdata.ensure_capacity(num_samples);

    switch (bits_per_sample) {
    case 8:
        read_samples_from_stream(stream, read_norm_sample_8, fdata, resampler, num_channels);
        break;
    case 16:
        read_samples_from_stream(stream, read_norm_sample_16, fdata, resampler, num_channels);
        break;
    case 24:
        read_samples_from_stream(stream, read_norm_sample_24, fdata, resampler, num_channels);
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
