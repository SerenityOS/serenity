/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleFile.h"

#include <AK/FixedArray.h>
#include <AK/Math.h>
#include <AK/Span.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Sample.h>

#include "RenderStruct.h"
#include "SampleBuffer.h"
#include "SampleFormatStruct.h"

SampleFile::SampleFile(StringView filename)
{
    m_filename = MUST(String::from_utf8(filename));
    MUST(m_buffer.create(SampleBuffer::BUFF_SIZE));
    load_metadata();
}

void SampleFile::load_metadata()
{
    auto maybe_loader = Audio::Loader::create(m_filename);
    if (maybe_loader.is_error())
        return;
    auto loader = maybe_loader.value();
    m_render_loader = loader;
    m_buffer_position = 0;
    m_buffered = false;
    m_format.format_name = loader->format_name();
    m_format.sample_rate = loader->sample_rate();
    m_format.num_channels = loader->num_channels();
    m_format.bits_per_sample = loader->bits_per_sample();
    m_samples = loader->total_samples();
}

String SampleFile::filename() { return m_filename; }

size_t SampleFile::length() { return m_samples; }

double SampleFile::duration()
{
    return static_cast<double>(m_samples) / static_cast<double>(m_format.sample_rate);
}

double SampleFile::sample_rate() { return m_format.sample_rate; }

RefPtr<Audio::Loader> SampleFile::ensure_render_loader()
{
    if (m_render_loader)
        return m_render_loader;

    auto maybe_loader = Audio::Loader::create(m_filename);
    if (maybe_loader.is_error())
        return nullptr;

    m_render_loader = maybe_loader.release_value();
    return m_render_loader;
}

RenderStruct SampleFile::rendered_sample_within_buffer(FixedArray<Audio::Sample>& buffer, size_t at)
{
    if (buffer.is_empty() || at >= buffer.size())
        return {};

    RenderStruct value;
    double sample = static_cast<double>(buffer[at].left);
    double magnitude = AK::abs(sample);

    if (sample >= 0.0) {
        value.rms_plus = magnitude;
        value.peak_plus = magnitude;
    } else {
        value.rms_minus = magnitude;
        value.peak_minus = magnitude;
    }

    return value;
}

void SampleFile::fill_render_columns(size_t first_sample, size_t total_samples, Span<RenderStruct> out)
{
    size_t num_cols = out.size();
    if (num_cols == 0)
        return;
    for (auto& rs : out)
        rs = {};
    if (total_samples == 0)
        return;

    auto loader = ensure_render_loader();
    if (!loader)
        return;
    if (loader->seek(first_sample).is_error())
        return;

    if (total_samples <= num_cols) {
        auto maybe_all = loader->get_more_samples(total_samples);
        if (maybe_all.is_error())
            return;
        auto all = maybe_all.release_value();
        for (size_t col = 0; col < num_cols; col++) {
            size_t idx = (col * total_samples) / num_cols;
            if (idx >= all.size())
                break;
            double s = static_cast<double>(all[idx].left);
            double mag = AK::abs(s);
            if (s >= 0.0) {
                out[col].peak_plus = mag;
                out[col].rms_plus = mag;
            } else {
                out[col].peak_minus = mag;
                out[col].rms_minus = mag;
            }
        }
        return;
    }

    double samples_per_col = static_cast<double>(total_samples) / static_cast<double>(num_cols);

    size_t col = 0;
    double sum_sq_pos = 0.0;
    double sum_sq_neg = 0.0;
    double peak_pos = 0.0;
    double peak_neg = 0.0;
    size_t pos_count = 0;
    size_t neg_count = 0;
    size_t samples_read = 0;

    auto finalize = [&]() {
        out[col].peak_plus = peak_pos;
        out[col].peak_minus = peak_neg;
        size_t n = pos_count + neg_count;
        if (n > 0) {
            if (pos_count > 0)
                out[col].rms_plus = AK::sqrt(sum_sq_pos / static_cast<double>(n));
            if (neg_count > 0)
                out[col].rms_minus = AK::sqrt(sum_sq_neg / static_cast<double>(n));
        }
        sum_sq_pos = 0.0;
        sum_sq_neg = 0.0;
        peak_pos = 0.0;
        peak_neg = 0.0;
        pos_count = 0;
        neg_count = 0;
    };

    while (samples_read < total_samples && col < num_cols) {
        size_t chunk = min(SampleBuffer::BUFF_SIZE, total_samples - samples_read);
        auto maybe_data = loader->get_more_samples(chunk);
        if (maybe_data.is_error())
            break;
        auto data = maybe_data.release_value();
        if (data.is_empty())
            break;

        for (size_t i = 0; i < data.size() && col < num_cols; i++) {
            size_t target_col = static_cast<size_t>(static_cast<double>(samples_read + i) / samples_per_col);
            if (target_col >= num_cols)
                target_col = num_cols - 1;

            while (col < target_col) {
                finalize();
                col++;
            }

            double s = static_cast<double>(data[i].left);
            double mag = AK::abs(s);
            if (s >= 0.0) {
                if (mag > peak_pos)
                    peak_pos = mag;
                sum_sq_pos += mag * mag;
                pos_count++;
            } else {
                if (mag > peak_neg)
                    peak_neg = mag;
                sum_sq_neg += mag * mag;
                neg_count++;
            }
        }
        samples_read += data.size();
    }

    if (col < num_cols)
        finalize();
}

void SampleFile::begin_loading_samples()
{
    m_stream_position = 0;
    auto maybe_loader = Audio::Loader::create(m_filename);
    if (maybe_loader.is_error()) {
        m_stream_loader = nullptr;
        return;
    }
    m_stream_loader = maybe_loader.release_value();
    if (m_stream_loader->seek(0).is_error())
        m_stream_loader = nullptr;
}

FixedArray<Audio::Sample> SampleFile::load_more_samples()
{
    if (!m_stream_loader)
        return SampleBuffer::null_samples();

    if (m_stream_position >= m_samples)
        return SampleBuffer::null_samples();

    auto maybe_samples = m_stream_loader->get_more_samples(SampleBuffer::BUFF_SIZE);
    if (maybe_samples.is_error())
        return SampleBuffer::null_samples();

    auto samples = maybe_samples.release_value();
    m_stream_position += samples.size();
    return samples;
}

RenderStruct SampleFile::rendered_sample_at(size_t position)
{
    RenderStruct result;

    auto loader = ensure_render_loader();
    if (!loader)
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
