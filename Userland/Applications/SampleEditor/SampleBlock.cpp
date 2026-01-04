/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleBlock.h"

#include <AK/Types.h>
#include <LibAudio/Sample.h>

#include "SampleBuffer.h"
#include "SampleFormatStruct.h"

NonnullRefPtr<SampleBlock> SampleBlock::create_null(size_t size, double duration)
{
    return adopt_ref(*new SampleBlock(size, duration));
}

SampleBlock::SampleBlock(size_t size, double duration)
{
    m_size = size;
    m_duration = duration;
    m_format.sample_rate = (double)size / duration;
}

size_t SampleBlock::length() { return m_size; }

double SampleBlock::duration() { return m_duration; }

double SampleBlock::sample_rate() { return m_format.sample_rate; }

String SampleBlock::description()
{
    return String::formatted("null").value();
}

RenderStruct SampleBlock::rendered_sample_at_valid(
    [[maybe_unused]] size_t position)
{
    return { 0, 0, 0, 0 };
}

void SampleBlock::begin_loading_samples() { m_position = 0; }

FixedArray<Audio::Sample> SampleBlock::load_more_samples()
{
    size_t remaining = m_size - m_position;
    size_t to_read = min(remaining, SampleBuffer::BUFF_SIZE);
    m_position += to_read;
    auto result = FixedArray<Audio::Sample>::create(to_read);
    if (result.is_error()) {
        return FixedArray<Audio::Sample>();
    }
    return result.release_value();
}
