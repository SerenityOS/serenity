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

#include <cassert>

#include "RenderStruct.h"
#include "SampleBlock.h"
#include "SampleBuffer.h"
#include "SampleFileBlock.h"
#include "SampleFormatStruct.h"
#include "SampleNullBlock.h"
#include "SampleSourceFile.h"

SampleBlockContainer::SampleBlockContainer()
{
    m_blocks = AK::Vector<NonnullRefPtr<SampleBlock>>();
}

ErrorOr<double> SampleBlockContainer::parse_and_insert(String json, double position)
{
    auto root = JsonValue::from_string(json);
    if (root.is_error()) {
        return Error::from_string_literal("Failed to parse clipboard JSON");
    }

    if (!root.value().is_object()) {
        return Error::from_string_literal("Clipboard JSON is not an object");
    }

    auto data = root.value().as_object();

    auto start_opt = data.get_double_with_precision_loss("start"sv);
    if (!start_opt.has_value()) {
        return Error::from_string_literal("Missing 'start' field in clipboard JSON");
    }

    auto end_opt = data.get_double_with_precision_loss("end"sv);
    if (!end_opt.has_value()) {
        return Error::from_string_literal("Missing 'end' field in clipboard JSON");
    }

    auto start = start_opt.value();
    auto end = end_opt.value();

    if (start < 0.0 || start > 1.0) {
        return Error::from_string_literal("Start value must be between 0.0 and 1.0");
    }

    if (end < 0.0 || end > 1.0) {
        return Error::from_string_literal("End value must be between 0.0 and 1.0");
    }

    if (start > end) {
        return Error::from_string_literal("Start value cannot be greater than end value");
    }

    if (start == end) {
        return Error::from_string_literal("Cannot paste zero-length selection");
    }

    struct source_info {
        ByteString path;
        long length;
        long file_start; // Start position within the source file
        long file_end;   // End position within the source file
        long rate;
        long channels;
        long bits;
        double duration;
    };

    auto source_infos = AK::Vector<source_info>();

    auto sources_root = data.get_array("sources"sv);
    if (!sources_root.has_value()) {
        return Error::from_string_literal("Missing 'sources' array in clipboard JSON");
    }

    {
        auto sources = sources_root.value();
        auto source_count = sources.size();

        for (size_t i = 0; i < source_count; i++) {
            auto source = sources.at(i).as_object();
            source_info info;
            info.path = source.get_byte_string("path"sv).value();
            info.length = source.get_i64("length"sv).value();
            auto file_start_opt = source.get_i64("start"sv);
            auto file_end_opt = source.get_i64("end"sv);
            info.file_start = file_start_opt.value_or(0);
            info.file_end = file_end_opt.value_or(info.length - 1);
            info.rate = source.get_i64("rate"sv).value();
            info.channels = source.get_i64("channels"sv).value();
            info.bits = source.get_i64("bits"sv).value();
            info.duration = (double)info.length / (double)info.rate;
            source_infos.append(info);
        }
    }

    double total_duration = 0.0;
    for (auto& info : source_infos) {
        total_duration += info.duration;
    }

    double start_seconds = total_duration * start;
    double end_seconds = total_duration * end;

    int start_block = -1, end_block = -1;
    double start_point, end_point;
    double input_accumulator = 0.0;
    for (size_t block_index = 0; block_index < source_infos.size(); block_index++) {
        auto& info = source_infos[block_index];
        double duration_mark = input_accumulator;
        input_accumulator += info.duration;
        if (-1 == start_block && input_accumulator > start_seconds) {
            start_block = block_index;
            start_point = (start_seconds - duration_mark) / info.duration;
        }
        if (-1 == end_block && input_accumulator > end_seconds) {
            end_block = block_index;
            end_point = (end_seconds - duration_mark) / info.duration;
            break;
        }
    }

    auto new_blocks = AK::Vector<NonnullRefPtr<SampleBlock>>();

    double position_seconds = position * duration();

    int position_block = -1;
    double position_point;
    double destination_accumulator = 0.0;
    for (size_t block_index = 0; block_index < m_blocks.size(); block_index++) {
        auto& block = m_blocks[block_index];
        double mark = destination_accumulator;
        auto block_duration = block->duration();
        destination_accumulator += block_duration;
        if (-1 == position_block && destination_accumulator > position_seconds) {
            position_block = block_index;
            position_point = (position_seconds - mark) / block_duration;
            break;
        }
    }

    AK::Vector<ByteString> failed_files;
    for (int block_index = start_block; block_index <= end_block; block_index++) {
        auto& info = source_infos[block_index];

        auto source_file_or_error = try_make_ref_counted<SampleSourceFile>(info.path);
        if (source_file_or_error.is_error()) {
            failed_files.append(info.path);
            continue;
        }
        auto source_file = source_file_or_error.release_value();

        size_t block_start = info.file_start;
        size_t block_end = info.file_end;

        if (block_index == start_block) {
            size_t block_length = block_end - block_start + 1;
            block_start = block_start + (size_t)(start_point * (double)block_length);
        }

        if (block_index == end_block) {
            size_t original_start = info.file_start;
            size_t original_end = info.file_end;
            size_t original_length = original_end - original_start + 1;
            block_end = original_start + (size_t)(end_point * (double)original_length);
        }

        auto file_block_or_error = try_make_ref_counted<SampleFileBlock>(
            source_file, block_start, block_end);
        if (file_block_or_error.is_error()) {
            failed_files.append(info.path);
            continue;
        }

        new_blocks.append(file_block_or_error.release_value());
    }

    if (!failed_files.is_empty()) {
        StringBuilder error_message;
        error_message.append("Failed to open the following source files:\n"sv);
        for (auto& file : failed_files) {
            error_message.append("  - "sv);
            error_message.append(file);
            error_message.append('\n');
        }
        return Error::from_string_view(error_message.string_view());
    }

    if (new_blocks.is_empty()) {
        return Error::from_string_literal("No valid blocks could be created from clipboard data");
    }

    bool is_initial_null_block = false;
    if (m_blocks.size() == 1) {
        auto* null_block = dynamic_cast<SampleNullBlock*>(m_blocks[0].ptr());
        if (null_block) {
            is_initial_null_block = true;
        }
    }

    if (m_blocks.size() > 0 && !is_initial_null_block) {
        auto existing_format = m_blocks[0]->format();
        for (auto& new_block : new_blocks) {
            auto new_format = new_block->format();
            if (new_format.sample_rate != existing_format.sample_rate) {
                return Error::from_string_literal("Cannot paste: sample rate mismatch. Paste content has different sample rate than existing content.");
            }
            if (new_format.num_channels != existing_format.num_channels) {
                return Error::from_string_literal("Cannot paste: channel count mismatch. Paste content has different number of channels than existing content.");
            }
            if (new_format.bits_per_sample != existing_format.bits_per_sample) {
                return Error::from_string_literal("Cannot paste: bit depth mismatch. Paste content has different bit depth than existing content.");
            }
        }
    }

    auto updated_blocks = AK::Vector<NonnullRefPtr<SampleBlock>>();

    if (position_block == -1) {
        for (auto& block : new_blocks) {
            updated_blocks.append(block);
        }
        for (auto& block : m_blocks) {
            updated_blocks.append(block);
        }
    } else {
        for (size_t i = 0; i < m_blocks.size(); i++) {
            if ((int)i == position_block) {
                auto& block = m_blocks[i];

                if (position_point <= 0.0) {
                    for (auto& new_block : new_blocks) {
                        updated_blocks.append(new_block);
                    }
                    updated_blocks.append(block);
                } else if (position_point >= 1.0) {
                    updated_blocks.append(block);
                    for (auto& new_block : new_blocks) {
                        updated_blocks.append(new_block);
                    }
                } else {
                    auto* file_block = dynamic_cast<SampleFileBlock*>(block.ptr());
                    if (file_block) {
                        auto split_result = file_block->split_at(position_point);
                        if (split_result.is_error()) {
                            updated_blocks.append(block);
                            for (auto& new_block : new_blocks) {
                                updated_blocks.append(new_block);
                            }
                        } else {
                            auto blocks = split_result.release_value();
                            updated_blocks.append(blocks[0]);
                            for (auto& new_block : new_blocks) {
                                updated_blocks.append(new_block);
                            }
                            updated_blocks.append(blocks[1]);
                        }
                    } else {
                        updated_blocks.append(block);
                        for (auto& new_block : new_blocks) {
                            updated_blocks.append(new_block);
                        }
                    }
                }
            } else {
                updated_blocks.append(m_blocks[i]);
            }
        }
    }

    m_blocks = updated_blocks;

    auto old_duration = m_total_duration;
    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_used = false;

    double pasted_duration = 0.0;
    for (auto& block : new_blocks) {
        pasted_duration += block->duration();
    }

    double insert_position_seconds = position * old_duration;
    double new_cursor_seconds = insert_position_seconds + pasted_duration;
    double new_cursor_position = new_cursor_seconds / m_total_duration;

    return new_cursor_position;
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

String SampleBlockContainer::sources()
{
    String result;
    bool first = true;
    for (auto& block : m_blocks) {
        String source = block->description();
        if (first) {
            result = String::formatted("[ {}", source).value();
        } else {
            result = String::formatted("{}, {}", result, source).value();
        }
        first = false;
    }
    result = String::formatted("{} ]", result).value();
    return result;
}

String SampleBlockContainer::sources_for_range(double start, double end)
{
    // Return descriptions for blocks overlapping the requested range.

    double start_seconds = start * m_total_duration;
    double end_seconds = end * m_total_duration;

    String result;
    bool first = true;
    double accumulated_duration = 0.0;

    for (auto& block : m_blocks) {
        double block_duration = block->duration();
        double block_start = accumulated_duration;
        double block_end = accumulated_duration + block_duration;

        if (block_end > start_seconds && block_start < end_seconds) {
            String source = block->description();
            if (first) {
                result = String::formatted("[ {}", source).value();
            } else {
                result = String::formatted("{}, {}", result, source).value();
            }
            first = false;
        }

        accumulated_duration += block_duration;
    }

    result = String::formatted("{} ]", result).value();
    return result;
}

SampleBlockContainer::SelectionInfo SampleBlockContainer::selection_info(double start, double end)
{
    double start_seconds = start * m_total_duration;
    double end_seconds = end * m_total_duration;

    double first_block_start = 0.0;
    double last_block_end = 0.0;
    double selection_duration = 0.0;

    String result;
    bool first = true;
    double accumulated_duration = 0.0;

    for (auto& block : m_blocks) {
        double block_duration = block->duration();
        double block_start = accumulated_duration;
        double block_end = accumulated_duration + block_duration;

        if (block_end > start_seconds && block_start < end_seconds) {
            if (first) {
                first_block_start = block_start;
                first = false;
            }
            last_block_end = block_end;
            selection_duration += block_duration;

            String source = block->description();
            if (result.is_empty()) {
                result = String::formatted("[ {}", source).value();
            } else {
                result = String::formatted("{}, {}", result, source).value();
            }
        }

        accumulated_duration += block_duration;
    }

    result = String::formatted("{} ]", result).value();

    double blocks_duration = last_block_end - first_block_start;
    double adjusted_start = (start_seconds - first_block_start) / blocks_duration;
    double adjusted_end = (end_seconds - first_block_start) / blocks_duration;

    adjusted_start = max(0.0, min(1.0, adjusted_start));
    adjusted_end = max(0.0, min(1.0, adjusted_end));

    return SelectionInfo {
        .sources = result,
        .adjusted_start = adjusted_start,
        .adjusted_end = adjusted_end
    };
}

ErrorOr<SampleBlockContainer::SelectionInfo> SampleBlockContainer::cut(double start, double end)
{
    if (start < 0.0 || end > 1.0 || start >= end) {
        return Error::from_string_literal("Invalid cut range");
    }

    auto selection = selection_info(start, end);

    double start_seconds = start * m_total_duration;
    double end_seconds = end * m_total_duration;

    auto new_blocks = AK::Vector<NonnullRefPtr<SampleBlock>>();
    double accumulated_duration = 0.0;

    for (auto& block : m_blocks) {
        double block_duration = block->duration();
        double block_start = accumulated_duration;
        double block_end = accumulated_duration + block_duration;

        if (block_end <= start_seconds) {
            new_blocks.append(block);
        } else if (block_start >= end_seconds) {
            new_blocks.append(block);
        } else {
            auto* file_block = dynamic_cast<SampleFileBlock*>(block.ptr());

            if (!file_block) {
                // Non-file blocks can't be split yet.
                new_blocks.append(block);
            } else {
                double cut_start_in_block = max(0.0, start_seconds - block_start);
                double cut_end_in_block = min(block_duration, end_seconds - block_start);

                bool cut_at_start = (cut_start_in_block <= 0.001);
                bool cut_at_end = (cut_end_in_block >= block_duration - 0.001);

                if (cut_at_start && cut_at_end) {
                } else if (cut_at_start) {
                    double fraction_to_remove = cut_end_in_block / block_duration;
                    size_t samples_to_remove = (size_t)(fraction_to_remove * (double)file_block->length());

                    size_t new_start = file_block->start() + samples_to_remove;
                    size_t new_end = file_block->end();

                    if (new_start <= new_end) {
                        auto trimmed_block = TRY(try_make_ref_counted<SampleFileBlock>(
                            file_block->file(), new_start, new_end));
                        new_blocks.append(trimmed_block);
                    }
                } else if (cut_at_end) {
                    double fraction_to_keep = cut_start_in_block / block_duration;
                    size_t samples_to_keep = (size_t)(fraction_to_keep * (double)file_block->length());

                    size_t new_start = file_block->start();
                    size_t new_end = file_block->start() + samples_to_keep - 1;

                    if (new_start <= new_end) {
                        auto trimmed_block = TRY(try_make_ref_counted<SampleFileBlock>(
                            file_block->file(), new_start, new_end));
                        new_blocks.append(trimmed_block);
                    }
                } else {
                    double start_fraction = cut_start_in_block / block_duration;
                    double end_fraction = cut_end_in_block / block_duration;

                    size_t block_length = file_block->end() - file_block->start() + 1;
                    size_t cut_start_sample = (size_t)(start_fraction * (double)block_length);
                    size_t cut_end_sample = (size_t)(end_fraction * (double)block_length);

                    size_t first_start = file_block->start();
                    size_t first_end = file_block->start() + cut_start_sample - 1;

                    if (first_start <= first_end) {
                        auto first_block = TRY(try_make_ref_counted<SampleFileBlock>(
                            file_block->file(), first_start, first_end));
                        new_blocks.append(first_block);
                    }

                    size_t second_start = file_block->start() + cut_end_sample;
                    size_t second_end = file_block->end();

                    if (second_start <= second_end) {
                        auto second_block = TRY(try_make_ref_counted<SampleFileBlock>(
                            file_block->file(), second_start, second_end));
                        new_blocks.append(second_block);
                    }
                }
            }
        }

        accumulated_duration += block_duration;
    }

    m_blocks = new_blocks;
    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_used = false;

    return selection;
}

void SampleBlockContainer::set_used() { m_used = true; }

bool SampleBlockContainer::used() { return m_used; }

bool SampleBlockContainer::is_initial_null_block() const
{
    if (m_blocks.size() != 1) {
        return false;
    }
    auto* null_block = dynamic_cast<SampleNullBlock const*>(m_blocks[0].ptr());
    return null_block != nullptr;
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

ErrorOr<SampleFormat> SampleBlockContainer::get_format()
{
    if (m_blocks.size() == 0) {
        return Error::from_string_literal("No blocks available");
    }

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
        assert(end > start);
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
        SampleBlock& block = m_blocks[m_stream_block];
        FixedArray<Audio::Sample> samples = block.load_more_samples();
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

    SampleBlock& block = m_blocks[m_stream_block];
    FixedArray<Audio::Sample> samples = block.load_more_samples();

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
