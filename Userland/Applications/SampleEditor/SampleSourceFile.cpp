/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleSourceFile.h"

#include <AK/FixedArray.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Sample.h>

#include "RenderStruct.h"
#include "SampleBuffer.h"
#include "SampleFile.h"
#include "SampleFormatStruct.h"

SampleSourceFile::SampleSourceFile(StringView filename)
{
    m_filename = String::formatted("{}", filename).value();
    MUST(m_buffer.create(SampleBuffer::BUFF_SIZE));
    load_metadata();
}

void SampleSourceFile::load_metadata()
{
    auto maybe_loader = Audio::Loader::create(m_filename);
    if (maybe_loader.is_error())
        return;
    m_loading = true;
    auto loader = maybe_loader.value();
    m_buffer_position = 0;
    m_buffered = false;
    m_format.format_name = loader->format_name();
    m_format.sample_rate = loader->sample_rate();
    m_format.num_channels = loader->num_channels();
    m_format.bits_per_sample = loader->bits_per_sample();
    m_samples = loader->total_samples();
}

String SampleSourceFile::filename() { return m_filename; }

size_t SampleSourceFile::length() { return m_samples; }

double SampleSourceFile::duration()
{
    return (double)m_samples / (double)m_format.sample_rate;
}

double SampleSourceFile::sample_rate() { return m_format.sample_rate; }

RenderStruct SampleSourceFile::rendered_sample_within_buffer(
    FixedArray<Audio::Sample>& buffer, size_t at)
{
    size_t size = buffer.size();
    size_t window = size / 4;

    size_t start = max(0, at - window);
    size_t end = max(size, at + window);

    size_t samples = max(window, 25);
    size_t increment = window / samples;

    size_t count_minus = 0;
    double total_square_minus = 0;
    double peak_minus = 0;
    size_t count_plus = 0;
    double total_square_plus = 0;
    double peak_plus = 0;

    for (size_t pos = start; pos < end; pos += increment) {
        double val = buffer[at].left;
        double square = val * val;
        double mod = AK::abs(val);

        if (val >= 0) {
            count_plus++;
            total_square_plus += square;
            peak_plus = AK::max(mod, peak_plus);
        } else {
            count_minus++;
            total_square_minus += square;
            peak_minus = AK::max(mod, peak_minus);
        }
    }

    RenderStruct value = { 0, 0, 0, 0 };

    if (count_plus) {
        value.RMS_plus = AK::sqrt(total_square_plus / ((double)count_plus));
        value.peak_plus = peak_plus;
    }

    if (count_minus) {
        value.RMS_minus = AK::sqrt(total_square_minus / ((double)count_minus));
        value.peak_minus = peak_minus;
    }

    return value;
}

void SampleSourceFile::begin_loading_samples() { m_stream_position = 0; }

FixedArray<Audio::Sample> SampleSourceFile::load_more_samples()
{
    auto maybe_loader = Audio::Loader::create(m_filename);
    if (maybe_loader.is_error())
        return SampleBuffer::null_samples();
    auto loader = maybe_loader.value();

    size_t number_of_samples = loader->total_samples();

    if (m_stream_position >= number_of_samples) {
        return SampleBuffer::null_samples();
    }

    auto maybe_error = loader->seek(m_stream_position);
    if (maybe_error.is_error()) {
        m_stream_position = 0;
        return SampleBuffer::null_samples();
    }

    auto maybe_samples = loader->get_more_samples(SampleBuffer::BUFF_SIZE);
    if (maybe_samples.is_error()) {
        m_stream_position = 0;
        return SampleBuffer::null_samples();
    }

    auto samples = maybe_samples.release_value();
    m_stream_position += samples.size();
    return samples;
}

RenderStruct SampleSourceFile::rendered_sample_at(size_t position)
{
    RenderStruct result = { 0, 0, 0, 0 };

    auto maybe_loader = Audio::Loader::create(m_filename);
    if (maybe_loader.is_error())
        return result;
    m_loading = true;
    auto loader = maybe_loader.value();

    auto error = loader->seek(m_buffer_position);
    if (error.is_error())
        return result;

    if (m_buffered && position >= m_buffer_position && position < m_buffer_position + m_buffer.size()) {
        auto at = position - m_buffer_position;
        result = rendered_sample_within_buffer(m_buffer, at);
    } else {
        auto error = loader->seek(position);
        if (error.is_error())
            return result;
        m_buffer_position = position;
        auto samples = loader->get_more_samples(SampleBuffer::BUFF_SIZE);
        m_buffered = !samples.is_error();
        if (m_buffered) {
            auto buffer = samples.release_value();
            m_buffer.swap(buffer);
        }
        result = rendered_sample_within_buffer(m_buffer, position - m_buffer_position);
    }

    return result;
}
