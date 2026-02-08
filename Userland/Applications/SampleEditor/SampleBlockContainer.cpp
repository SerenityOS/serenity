/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleBlockContainer.h"

#include <AK/ByteString.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Math.h>
#include <AK/RefPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibAudio/Sample.h>

#include "RenderStruct.h"
#include "SampleBlock.h"
#include "SampleBuffer.h"
#include "SampleFileBlock.h"
#include "SampleFormatStruct.h"
#include "SampleSourceFile.h"

SampleBlockContainer::SampleBlockContainer()
{
    m_blocks = AK::Vector<NonnullRefPtr<SampleBlock>>();
}

void SampleBlockContainer::set(NonnullRefPtr<SampleBlock> block)
{
    m_blocks = AK::Vector<NonnullRefPtr<SampleBlock>>();
    m_blocks.append(block);
    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_used = false;
}

void SampleBlockContainer::append(NonnullRefPtr<SampleBlock> block)
{

    m_blocks.append(block);
    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_used = false;
}

ErrorOr<SampleFormat> SampleBlockContainer::get_format()
{
    auto first_format = m_blocks[0]->format();

    for (size_t i = 1; i < m_blocks.size(); i++) {
        auto block_format = m_blocks[i]->format();

        if (block_format.sample_rate != first_format.sample_rate) {
            return Error::from_string_literal("Blocks have inconsistent sample rates");
        }
        if (block_format.num_channels != first_format.num_channels) {
            return Error::from_string_literal("Blocks have inconsistent channel counts");
        }
        if (block_format.bits_per_sample != first_format.bits_per_sample) {
            return Error::from_string_literal("Blocks have inconsistent bit depths");
        }
    }

    return first_format;
}

size_t SampleBlockContainer::calc_length()
{
    size_t length = 0;
    for (auto& block : m_blocks) {
        length += block->length();
    }
    return length;
}

double SampleBlockContainer::calc_duration()
{
    double duration = 0.0;
    for (auto& block : m_blocks) {
        duration += block->duration();
    }
    return duration;
}

RenderStruct SampleBlockContainer::rendered_sample_at(double position)
{
    if (position < 0 || position > 1 || m_total_length == 0 || m_blocks.size() == 0)
        return { 0, 0, 0, 0 };

    size_t total = 0;
    double start = 0;
    double end = 0;

    for (auto& block : m_blocks) {
        size_t length = block->length();
        total += length;
        end = (double)total / (double)m_total_length;
        VERIFY(end > start);
        if (position <= end) {
            double within = (position - start) / (end - start);
            size_t sample_position = (size_t)(((double)length) * within);
            return block->rendered_sample_at(sample_position);
        }
        start = end;
    }

    return { 0, 0, 0, 0 };
}

void SampleBlockContainer::begin_loading_samples()
{
    m_stream_block = 0;
    m_stream_position = 0;
    for (auto& block : m_blocks) {
        block->begin_loading_samples();
    }
}

void SampleBlockContainer::begin_loading_samples_at(double start_position)
{
    double start_seconds = start_position * m_total_duration;
    double accumulated_duration = 0.0;

    for (size_t i = 0; i < m_blocks.size(); i++) {
        double block_duration = m_blocks[i]->duration();
        if (accumulated_duration + block_duration > start_seconds) {
            m_stream_block = i;
            m_stream_position = 0;

            for (auto& block : m_blocks) {
                block->begin_loading_samples();
            }

            return;
        }
        accumulated_duration += block_duration;
    }

    m_stream_block = m_blocks.size() > 0 ? m_blocks.size() - 1 : 0;
    m_stream_position = 0;
    for (auto& block : m_blocks) {
        block->begin_loading_samples();
    }
}

FixedArray<Audio::Sample> SampleBlockContainer::load_more_samples()
{
    size_t num_blocks = m_blocks.size();
    if (m_stream_block < num_blocks) {
        FixedArray<Audio::Sample> samples = m_blocks[m_stream_block]->load_more_samples();
        if (samples.size() == 0) {
            m_stream_block++;
            m_stream_position = 0;
            return load_more_samples();
        }
        return samples;
    } else {
        return SampleBuffer::null_samples();
    }
}

FixedArray<Audio::Sample> SampleBlockContainer::load_more_samples_in_range(double start, double end, size_t& samples_loaded)
{
    if (m_blocks.size() == 0 || m_total_length == 0)
        return SampleBuffer::null_samples();

    size_t start_sample = (size_t)(start * (double)m_total_length);
    size_t end_sample = (size_t)(end * (double)m_total_length);
    size_t total_samples_in_range = end_sample - start_sample;

    if (samples_loaded >= total_samples_in_range)
        return SampleBuffer::null_samples();

    if (m_stream_block >= m_blocks.size())
        return SampleBuffer::null_samples();

    FixedArray<Audio::Sample> samples = m_blocks[m_stream_block]->load_more_samples();

    if (samples.size() == 0) {
        m_stream_block++;
        m_stream_position = 0;
        return load_more_samples_in_range(start, end, samples_loaded);
    }

    size_t current_absolute_position = 0;
    for (size_t i = 0; i < m_stream_block; i++) {
        current_absolute_position += m_blocks[i]->length();
    }
    current_absolute_position += m_stream_position;

    size_t skip_samples = 0;
    if (current_absolute_position < start_sample) {
        size_t samples_until_start = start_sample - current_absolute_position;
        skip_samples = min(samples_until_start, samples.size());
    }

    m_stream_position += samples.size();

    size_t samples_available = samples.size() - skip_samples;
    size_t samples_remaining = total_samples_in_range - samples_loaded;
    size_t samples_to_use = min(samples_available, samples_remaining);

    if (samples_to_use == 0 && samples_remaining > 0) {
        return load_more_samples_in_range(start, end, samples_loaded);
    }

    if (skip_samples > 0 || samples_to_use < samples.size()) {
        auto trimmed = FixedArray<Audio::Sample>::create(samples_to_use);
        if (trimmed.is_error())
            return SampleBuffer::null_samples();

        auto result = trimmed.release_value();
        for (size_t i = 0; i < samples_to_use; i++) {
            result[i] = samples[skip_samples + i];
        }
        samples_loaded += samples_to_use;
        return result;
    }

    samples_loaded += samples.size();
    return samples;
}

size_t SampleBlockContainer::length() { return m_total_length; }

double SampleBlockContainer::duration() { return m_total_duration; }

double SampleBlockContainer::sample_rate()
{
    if (m_blocks.size() > 0) {
        return m_blocks[0]->sample_rate();
    }
    return 44100.0; // Fallback when no blocks exist
}

bool SampleBlockContainer::used() { return m_used; }

void SampleBlockContainer::set_used() { m_used = true; }
