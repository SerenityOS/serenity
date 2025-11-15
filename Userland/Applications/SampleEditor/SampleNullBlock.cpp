/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleNullBlock.h"

#include <AK/Types.h>
#include <LibAudio/Sample.h>

#include "SampleBlock.h"
#include "SampleBuffer.h"
#include "SampleFormatStruct.h"

SampleNullBlock::SampleNullBlock(size_t size, double duration)
{
    m_size = size;
    m_duration = duration;
    m_format.sample_rate = (double)size / duration;
}

size_t SampleNullBlock::length() { return m_size; }

double SampleNullBlock::duration() { return m_duration; }

double SampleNullBlock::sample_rate() { return m_format.sample_rate; }

String SampleNullBlock::description()
{
    return String::formatted("null").value();
}

RenderStruct SampleNullBlock::rendered_sample_at_valid(
    [[maybe_unused]] size_t position)
{
    return { 0, 0, 0, 0 };
}

void SampleNullBlock::begin_loading_samples() { m_position = 0; }

FixedArray<Audio::Sample> SampleNullBlock::load_more_samples()
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
