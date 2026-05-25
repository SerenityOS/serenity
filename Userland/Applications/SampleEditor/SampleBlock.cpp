/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleBlock.h"

#include <AK/RefPtr.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Sample.h>

#include "SampleBuffer.h"

SampleBlock::SampleBlock(NonnullRefPtr<SampleFile> file, size_t start, size_t end)
    : m_file(file)
    , m_start(start)
    , m_end(min(end, file->length()))
    , m_length(m_end >= start ? m_end - start + 1 : 0)
    , m_format(file->format())
{
}

size_t SampleBlock::length() { return m_length; }

double SampleBlock::duration()
{
    return static_cast<double>(m_length) / static_cast<double>(m_format.sample_rate);
}

String SampleBlock::description()
{
    return String::formatted(
        "{{ \"path\": \"{}\", \"length\":{}, \"start\":{}, \"end\":{}, \"rate\":{}, \"channels\":{}, \"bits\":{} }}",
        m_file->filename(), m_length, m_start, m_end,
        m_format.sample_rate, m_format.num_channels, m_format.bits_per_sample)
        .value();
}

double SampleBlock::sample_rate() { return m_format.sample_rate; }

RenderStruct SampleBlock::rendered_sample_at(size_t position)
{
    if (position >= length())
        return {};
    return m_file->rendered_sample_at(position + m_start);
}

void SampleBlock::begin_loading_samples()
{
    m_stream_position = m_start;
    auto maybe_loader = Audio::Loader::create(m_file->filename());
    if (maybe_loader.is_error()) {
        m_stream_loader = nullptr;
        return;
    }

    m_stream_loader = maybe_loader.release_value();
    auto maybe_error = m_stream_loader->seek(m_stream_position);
    if (maybe_error.is_error())
        m_stream_loader = nullptr;
}

FixedArray<Audio::Sample> SampleBlock::load_more_samples()
{
    if (m_stream_position > m_end)
        return SampleBuffer::null_samples();

    size_t remaining_in_block = m_end - m_stream_position + 1;
    size_t samples_to_load = min(remaining_in_block, SampleBuffer::BUFF_SIZE);

    if (!m_stream_loader) {
        auto maybe_loader = Audio::Loader::create(m_file->filename());
        if (maybe_loader.is_error())
            return SampleBuffer::null_samples();
        m_stream_loader = maybe_loader.release_value();

        auto maybe_error = m_stream_loader->seek(m_stream_position);
        if (maybe_error.is_error())
            return SampleBuffer::null_samples();
    }

    auto maybe_samples = m_stream_loader->get_more_samples(samples_to_load);
    if (maybe_samples.is_error())
        return SampleBuffer::null_samples();

    auto samples = maybe_samples.release_value();
    m_stream_position += samples.size();
    return samples;
}

ErrorOr<Array<NonnullRefPtr<SampleBlock>, 2>> SampleBlock::split_at(double position)
{
    if (position <= 0.0 || position >= 1.0)
        return Error::from_string_literal("Split position must be between 0.0 and 1.0 (exclusive)");

    size_t total_length = m_end - m_start + 1;
    size_t split_offset = static_cast<size_t>(position * static_cast<double>(total_length));
    size_t split_point = m_start + split_offset;

    if (split_point <= m_start || split_point >= m_end)
        return Error::from_string_literal("Split point would create empty block");

    auto first_block = TRY(try_make_ref_counted<SampleBlock>(m_file, m_start, split_point - 1));
    auto second_block = TRY(try_make_ref_counted<SampleBlock>(m_file, split_point, m_end));

    return Array<NonnullRefPtr<SampleBlock>, 2> { first_block, second_block };
}
