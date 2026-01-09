/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleFileBlock.h"

#include <AK/RefPtr.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Sample.h>

#include "SampleBuffer.h"
#include "SampleFormatStruct.h"

SampleFileBlock::SampleFileBlock(NonnullRefPtr<SampleSourceFile> file, size_t start,
    size_t end)
    : m_file(file)
{
    m_file = file;
    auto file_length = m_file->length();
    m_start = start;
    m_end = (file_length > end) ? end : file_length;
    m_length = (m_end >= m_start) ? (m_end - m_start + 1) : 0;
    m_format = m_file->format();
}

size_t SampleFileBlock::length() { return m_length; }

double SampleFileBlock::duration()
{
    return (double)m_length / (double)m_format.sample_rate;
}

String SampleFileBlock::description()
{
    int rate = m_format.sample_rate;
    int channels = m_format.num_channels;
    int bits = m_format.bits_per_sample;

    return String::formatted(
        "{{ \"path\": \"{}\", \"length\":{}, \"start\":{}, \"end\":{}, \"rate\":{}, \"channels\":{}, \"bits\":{} }}",
        m_file->filename(), m_length, m_start, m_end, rate, channels, bits)
        .value();
}

double SampleFileBlock::sample_rate() { return m_format.sample_rate; }

RenderStruct SampleFileBlock::rendered_sample_at_valid(size_t position)
{
    return m_file->rendered_sample_at(position + m_start);
}

void SampleFileBlock::begin_loading_samples()
{
    m_stream_position = m_start;
}

FixedArray<Audio::Sample> SampleFileBlock::load_more_samples()
{
    if (m_stream_position > m_end) {
        return SampleBuffer::null_samples();
    }

    size_t remaining_in_block = m_end - m_stream_position + 1;
    size_t samples_to_load = min(remaining_in_block, SampleBuffer::BUFF_SIZE);

    auto maybe_loader = Audio::Loader::create(m_file->filename());
    if (maybe_loader.is_error())
        return SampleBuffer::null_samples();
    auto loader = maybe_loader.value();

    auto maybe_error = loader->seek(m_stream_position);
    if (maybe_error.is_error())
        return SampleBuffer::null_samples();

    auto maybe_samples = loader->get_more_samples(samples_to_load);
    if (maybe_samples.is_error())
        return SampleBuffer::null_samples();

    auto samples = maybe_samples.release_value();
    m_stream_position += samples.size();
    return samples;
}

ErrorOr<Array<NonnullRefPtr<SampleFileBlock>, 2>> SampleFileBlock::split_at(double position)
{
    if (position <= 0.0 || position >= 1.0) {
        return Error::from_string_literal("Split position must be between 0.0 and 1.0 (exclusive)");
    }

    size_t total_length = m_end - m_start + 1;
    size_t split_offset = static_cast<size_t>(position * total_length);
    size_t split_point = m_start + split_offset;

    if (split_point <= m_start || split_point >= m_end) {
        return Error::from_string_literal("Split point would create empty block");
    }

    auto first_block = TRY(try_make_ref_counted<SampleFileBlock>(m_file, m_start, split_point - 1));

    auto second_block = TRY(try_make_ref_counted<SampleFileBlock>(m_file, split_point, m_end));

    return Array<NonnullRefPtr<SampleFileBlock>, 2> { first_block, second_block };
}
