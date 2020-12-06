/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Platform.h>
#include <stddef.h>
#include <stdarg.h>
#include <sys/ioctl.h>

namespace Audio {

namespace PCM {

enum class SampleFormat {
    Unknown = 0,
    S8_LE,
    S8_BE,
    S16_LE,
    S16_BE,
    S24_LE,
    S24_BE,
    S24_32_LE,
    S24_32_BE,
    S32_LE,
    S32_BE,
    U8_LE,
    U8_BE,
    U16_LE,
    U16_BE,
    U24_LE,
    U24_BE,
    U24_32_LE,
    U24_32_BE,
    U32_LE,
    U32_BE,
    F32_LE,
    F32_BE,
    F64_LE,
    F64_BE,
};

ALWAYS_INLINE bool is_little_endian(SampleFormat format)
{
    switch (format) {
    case SampleFormat::S8_LE:
    case SampleFormat::S16_LE:
    case SampleFormat::S24_LE:
    case SampleFormat::S24_32_LE:
    case SampleFormat::S32_LE:
    case SampleFormat::U8_LE:
    case SampleFormat::U16_LE:
    case SampleFormat::U24_LE:
    case SampleFormat::U24_32_LE:
    case SampleFormat::U32_LE:
    case SampleFormat::F32_LE:
    case SampleFormat::F64_LE:
        return true;
    default:
        return false;
    }
}

ALWAYS_INLINE bool is_big_endian(SampleFormat format)
{
    switch (format) {
    case SampleFormat::S8_BE:
    case SampleFormat::S16_BE:
    case SampleFormat::S24_BE:
    case SampleFormat::S24_32_BE:
    case SampleFormat::S32_BE:
    case SampleFormat::U8_BE:
    case SampleFormat::U16_BE:
    case SampleFormat::U24_BE:
    case SampleFormat::U24_32_BE:
    case SampleFormat::U32_BE:
    case SampleFormat::F32_BE:
    case SampleFormat::F64_BE:
        return true;
    default:
        return false;
    }
}

ALWAYS_INLINE bool is_signed(SampleFormat format)
{
    switch (format) {
    case SampleFormat::S8_LE:
    case SampleFormat::S8_BE:
    case SampleFormat::S16_LE:
    case SampleFormat::S16_BE:
    case SampleFormat::S24_LE:
    case SampleFormat::S24_BE:
    case SampleFormat::S24_32_LE:
    case SampleFormat::S24_32_BE:
    case SampleFormat::S32_LE:
    case SampleFormat::S32_BE:
        return true;
    default:
        return false;
    }
}

ALWAYS_INLINE bool is_unsigned(SampleFormat format)
{
    switch (format) {
    case SampleFormat::U8_LE:
    case SampleFormat::U8_BE:
    case SampleFormat::U16_LE:
    case SampleFormat::U16_BE:
    case SampleFormat::U24_LE:
    case SampleFormat::U24_BE:
    case SampleFormat::U24_32_LE:
    case SampleFormat::U24_32_BE:
    case SampleFormat::U32_LE:
    case SampleFormat::U32_BE:
        return true;
    default:
        return false;
    }
}

ALWAYS_INLINE bool is_float(SampleFormat format)
{
    switch (format) {
    case SampleFormat::F32_LE:
    case SampleFormat::F32_BE:
    case SampleFormat::F64_LE:
    case SampleFormat::F64_BE:
        return true;
    default:
        return false;
    }
}

ALWAYS_INLINE size_t bytes_per_sample(SampleFormat format)
{
    switch (format) {
    case SampleFormat::S8_LE:
    case SampleFormat::S8_BE:
    case SampleFormat::U8_LE:
    case SampleFormat::U8_BE:
        return 1;
    case SampleFormat::S16_LE:
    case SampleFormat::S16_BE:
    case SampleFormat::U16_LE:
    case SampleFormat::U16_BE:
        return 2;
    case SampleFormat::S24_LE:
    case SampleFormat::S24_BE:
    case SampleFormat::U24_LE:
    case SampleFormat::U24_BE:
        return 3;
    case SampleFormat::S32_LE:
    case SampleFormat::S32_BE:
    case SampleFormat::U32_LE:
    case SampleFormat::U32_BE:
    case SampleFormat::S24_32_LE:
    case SampleFormat::S24_32_BE:
    case SampleFormat::U24_32_LE:
    case SampleFormat::U24_32_BE:
    case SampleFormat::F32_LE:
    case SampleFormat::F32_BE:
        return 4;
    case SampleFormat::F64_LE:
    case SampleFormat::F64_BE:
        return 8;
    default:
        return 0;
    }
}

ALWAYS_INLINE size_t significant_bits_per_sample(SampleFormat format)
{
    switch (format) {
    case SampleFormat::S8_LE:
    case SampleFormat::S8_BE:
    case SampleFormat::U8_LE:
    case SampleFormat::U8_BE:
        return 8;
    case SampleFormat::S16_LE:
    case SampleFormat::S16_BE:
    case SampleFormat::U16_LE:
    case SampleFormat::U16_BE:
        return 16;
    case SampleFormat::S24_LE:
    case SampleFormat::S24_BE:
    case SampleFormat::U24_LE:
    case SampleFormat::U24_BE:
    case SampleFormat::S24_32_LE:
    case SampleFormat::S24_32_BE:
    case SampleFormat::U24_32_LE:
    case SampleFormat::U24_32_BE:
        return 24;
    case SampleFormat::S32_LE:
    case SampleFormat::S32_BE:
    case SampleFormat::U32_LE:
    case SampleFormat::U32_BE:
    case SampleFormat::F32_LE:
    case SampleFormat::F32_BE:
        return 32;
    case SampleFormat::F64_LE:
    case SampleFormat::F64_BE:
        return 64;
    default:
        return 0;
    }
}

ALWAYS_INLINE size_t bytes_per_frame(SampleFormat format, unsigned channels)
{
    return bytes_per_sample(format) * channels;
}

ALWAYS_INLINE size_t bytes_per_second(unsigned rate, SampleFormat format, unsigned channels)
{
    return rate * bytes_per_frame(format, channels);
}

ALWAYS_INLINE u64 time_to_frames(u64 ns, unsigned rate)
{
    // TODO: deal with overflows
    return ((u64)rate * ns) / 1000000000ull;
}

ALWAYS_INLINE u64 frames_to_time(u64 frames, unsigned rate)
{
    // TODO: deal with overflows
    return (frames * 1000000000ull) / (u64)rate;
}

enum class SampleLayout {
    Unknown = 0,
    Interleaved,
    NonInterleaved,
};

}

struct IOCtlJsonParams {
    const void* in_buffer;
    size_t in_buffer_size;
    void* out_buffer;
    size_t out_buffer_size;
};

struct IOCtlSetPCMHwParams {
    PCM::SampleFormat format { PCM::SampleFormat::Unknown };
    PCM::SampleLayout layout { PCM::SampleLayout::Unknown };
    unsigned rate { 0 };
    unsigned channels { 0 };
    unsigned periods { 0 };
    unsigned periods_trigger { 0 };
    u64 period_ns { 0 };

    bool is_null() const
    {
        // NOTE: periods_trigger may be 0!
        return format == PCM::SampleFormat::Unknown
            || layout == PCM::SampleLayout::Unknown
            || rate == 0
            || channels == 0
            || periods == 0
            || period_ns == 0;
    }
};

enum class StreamType {
    Unknown = 0,
    Playback,
    Record
};

enum class IOCtl {
    SELECT_STREAM,
    GET_PCM_HW_PARAMS,
    SET_PCM_HW_PARAMS,
    PCM_PREPARE,
};

template<typename ParamType>
ALWAYS_INLINE int audio_ioctl(int fd, IOCtl request, ParamType& params)
{
    return ::ioctl(fd, (unsigned)request, &params);
}

ALWAYS_INLINE int audio_ioctl(int fd, IOCtl request, unsigned arg)
{
    return ::ioctl(fd, (unsigned)request, (FlatPtr)arg);
}

}
