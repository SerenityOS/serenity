/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleDocument.h"

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
#include "SampleFile.h"
#include "SampleFormatStruct.h"

SampleDocument::SampleDocument() = default;

void SampleDocument::invalidate_render_lookup()
{
    m_block_lookup = {};
}

ErrorOr<SampleDocument::ClipboardData> SampleDocument::parse_clipboard_json(String const& json)
{
    auto root = JsonValue::from_string(json);
    if (root.is_error())
        return Error::from_string_literal("Failed to parse clipboard JSON");

    if (!root.value().is_object())
        return Error::from_string_literal("Clipboard JSON is not an object");

    auto data = root.value().as_object();

    auto start_opt = data.get_double_with_precision_loss("start"sv);
    if (!start_opt.has_value())
        return Error::from_string_literal("Missing 'start' field in clipboard JSON");

    auto end_opt = data.get_double_with_precision_loss("end"sv);
    if (!end_opt.has_value())
        return Error::from_string_literal("Missing 'end' field in clipboard JSON");

    double start = start_opt.value();
    double end = end_opt.value();

    if (start < 0.0 || start > 1.0)
        return Error::from_string_literal("Start value must be between 0.0 and 1.0");
    if (end < 0.0 || end > 1.0)
        return Error::from_string_literal("End value must be between 0.0 and 1.0");
    if (start > end)
        return Error::from_string_literal("Start value cannot be greater than end value");
    if (start == end)
        return Error::from_string_literal("Cannot paste zero-length selection");

    auto sources_root = data.get_array("sources"sv);
    if (!sources_root.has_value())
        return Error::from_string_literal("Missing 'sources' array in clipboard JSON");

    Vector<SourceInfo> sources;
    auto& sources_array = sources_root.value();
    for (size_t i = 0; i < sources_array.size(); i++) {
        auto source = sources_array.at(i).as_object();
        SourceInfo info;
        info.path = source.get_byte_string("path"sv).value();
        info.length = source.get_i64("length"sv).value();
        info.file_start = source.get_i64("start"sv).value_or(0);
        info.file_end = source.get_i64("end"sv).value_or(info.length - 1);
        info.rate = source.get_i64("rate"sv).value();
        info.channels = source.get_i64("channels"sv).value();
        info.bits = source.get_i64("bits"sv).value();
        info.duration = static_cast<double>(info.length) / static_cast<double>(info.rate);
        sources.append(info);
    }

    double total_duration = 0.0;
    for (auto& info : sources)
        total_duration += info.duration;

    return ClipboardData { start, end, move(sources), total_duration };
}

ErrorOr<Vector<NonnullRefPtr<SampleBlock>>> SampleDocument::load_blocks_from_clipboard(
    ClipboardData const& clipboard,
    size_t start_block_index, size_t end_block_index,
    double start_point, double end_point)
{
    Vector<NonnullRefPtr<SampleBlock>> new_blocks;
    Vector<ByteString> failed_files;

    for (size_t block_index = start_block_index; block_index <= end_block_index; block_index++) {
        auto& info = clipboard.sources[block_index];

        auto source_file_or_error = try_make_ref_counted<SampleFile>(info.path);
        if (source_file_or_error.is_error()) {
            failed_files.append(info.path);
            continue;
        }
        auto source_file = source_file_or_error.release_value();

        size_t block_start = static_cast<size_t>(info.file_start);
        size_t block_end = static_cast<size_t>(info.file_end);

        if (block_index == start_block_index) {
            size_t block_length = block_end - block_start + 1;
            block_start += static_cast<size_t>(start_point * static_cast<double>(block_length));
        }

        if (block_index == end_block_index) {
            size_t original_start = static_cast<size_t>(info.file_start);
            size_t original_end = static_cast<size_t>(info.file_end);
            size_t original_length = original_end - original_start + 1;
            block_end = original_start + static_cast<size_t>(end_point * static_cast<double>(original_length));
        }

        auto block_or_error = try_make_ref_counted<SampleBlock>(source_file, block_start, block_end);
        if (block_or_error.is_error()) {
            failed_files.append(info.path);
            continue;
        }

        new_blocks.append(block_or_error.release_value());
    }

    if (!failed_files.is_empty()) {
        for (auto& file : failed_files)
            dbgln("SampleDocument: failed to open source file: {}", file);
        return Error::from_string_literal("Failed to open one or more source files");
    }

    if (new_blocks.is_empty())
        return Error::from_string_literal("No valid blocks could be created from clipboard data");

    return new_blocks;
}

ErrorOr<void> SampleDocument::splice_blocks(
    Vector<NonnullRefPtr<SampleBlock>> const& new_blocks,
    Optional<size_t> position_block_index,
    double position_point)
{
    Vector<NonnullRefPtr<SampleBlock>> updated_blocks;

    if (!position_block_index.has_value()) {
        for (auto& block : new_blocks)
            updated_blocks.append(block);
        for (auto& block : m_blocks)
            updated_blocks.append(block);
    } else {
        for (size_t i = 0; i < m_blocks.size(); i++) {
            if (i == position_block_index.value()) {
                auto& block = m_blocks[i];

                if (position_point <= 0.0) {
                    for (auto& new_block : new_blocks)
                        updated_blocks.append(new_block);
                    updated_blocks.append(block);
                } else if (position_point >= 1.0) {
                    updated_blocks.append(block);
                    for (auto& new_block : new_blocks)
                        updated_blocks.append(new_block);
                } else {
                    auto split_result = block->split_at(position_point);
                    if (split_result.is_error()) {
                        updated_blocks.append(block);
                        for (auto& new_block : new_blocks)
                            updated_blocks.append(new_block);
                    } else {
                        auto blocks = split_result.release_value();
                        updated_blocks.append(blocks[0]);
                        for (auto& new_block : new_blocks)
                            updated_blocks.append(new_block);
                        updated_blocks.append(blocks[1]);
                    }
                }
            } else {
                updated_blocks.append(m_blocks[i]);
            }
        }
    }

    m_blocks = move(updated_blocks);
    return {};
}

ErrorOr<double> SampleDocument::parse_and_insert(String json, double position)
{
    auto clipboard = TRY(parse_clipboard_json(json));

    double start_seconds = clipboard.total_duration * clipboard.start;
    double end_seconds = clipboard.total_duration * clipboard.end;

    Optional<size_t> start_block_index;
    Optional<size_t> end_block_index;
    double start_point { 0.0 };
    double end_point { 0.0 };
    double input_accumulator = 0.0;

    for (size_t i = 0; i < clipboard.sources.size(); i++) {
        auto& info = clipboard.sources[i];
        double duration_mark = input_accumulator;
        input_accumulator += info.duration;
        if (!start_block_index.has_value() && input_accumulator > start_seconds) {
            start_block_index = i;
            start_point = (start_seconds - duration_mark) / info.duration;
        }
        if (!end_block_index.has_value() && input_accumulator > end_seconds) {
            end_block_index = i;
            end_point = (end_seconds - duration_mark) / info.duration;
            break;
        }
    }

    if (!start_block_index.has_value() || !end_block_index.has_value())
        return Error::from_string_literal("Clipboard time range does not map to any source block");

    auto new_blocks = TRY(load_blocks_from_clipboard(clipboard, start_block_index.value(), end_block_index.value(), start_point, end_point));

    if (!m_blocks.is_empty()) {
        auto existing_format = m_blocks[0]->format();
        for (auto& block : new_blocks) {
            auto fmt = block->format();
            if (fmt.sample_rate != existing_format.sample_rate)
                return Error::from_string_literal("Cannot paste: sample rate mismatch.");
            if (fmt.num_channels != existing_format.num_channels)
                return Error::from_string_literal("Cannot paste: channel count mismatch.");
            if (fmt.bits_per_sample != existing_format.bits_per_sample)
                return Error::from_string_literal("Cannot paste: bit depth mismatch.");
        }
    }

    Optional<size_t> position_block_index;
    double position_point { 0.0 };
    double position_seconds = position * duration();
    double destination_accumulator = 0.0;

    for (size_t i = 0; i < m_blocks.size(); i++) {
        auto& block = m_blocks[i];
        double mark = destination_accumulator;
        destination_accumulator += block->duration();
        if (!position_block_index.has_value() && destination_accumulator > position_seconds) {
            position_block_index = i;
            position_point = (position_seconds - mark) / block->duration();
            break;
        }
    }

    auto old_duration = m_total_duration;
    TRY(splice_blocks(new_blocks, position_block_index, position_point));

    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_render_valid = false;
    invalidate_render_lookup();

    double pasted_duration = 0.0;
    for (auto& block : new_blocks)
        pasted_duration += block->duration();

    double insert_position_seconds = position * old_duration;
    double new_cursor_seconds = insert_position_seconds + pasted_duration;
    return new_cursor_seconds / m_total_duration;
}

void SampleDocument::set(NonnullRefPtr<SampleBlock> block)
{
    m_blocks.clear();
    m_blocks.append(block);
    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_render_valid = false;
    invalidate_render_lookup();
}

void SampleDocument::append(NonnullRefPtr<SampleBlock> block)
{
    m_blocks.append(block);
    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_render_valid = false;
    invalidate_render_lookup();
}

void SampleDocument::clear()
{
    m_blocks.clear();
    m_total_length = 0;
    m_total_duration = 0.0;
    m_render_valid = false;
    invalidate_render_lookup();
}

String SampleDocument::sources()
{
    StringBuilder builder;
    builder.append("[ "sv);
    bool first = true;
    for (auto& block : m_blocks) {
        if (!first)
            builder.append(", "sv);
        builder.append(block->description());
        first = false;
    }
    builder.append(" ]"sv);
    return MUST(builder.to_string());
}

String SampleDocument::sources_for_range(double start, double end)
{
    double start_seconds = start * m_total_duration;
    double end_seconds = end * m_total_duration;

    StringBuilder builder;
    builder.append("[ "sv);
    bool first = true;
    double accumulated_duration = 0.0;

    for (auto& block : m_blocks) {
        double block_duration = block->duration();
        double block_start = accumulated_duration;
        double block_end = accumulated_duration + block_duration;

        if (block_end > start_seconds && block_start < end_seconds) {
            if (!first)
                builder.append(", "sv);
            builder.append(block->description());
            first = false;
        }

        accumulated_duration += block_duration;
    }

    builder.append(" ]"sv);
    return MUST(builder.to_string());
}

SampleDocument::SelectionInfo SampleDocument::selection_info(double start, double end)
{
    double start_seconds = start * m_total_duration;
    double end_seconds = end * m_total_duration;

    double first_block_start = 0.0;
    double last_block_end = 0.0;

    StringBuilder builder;
    builder.append("[ "sv);
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
            } else {
                builder.append(", "sv);
            }
            last_block_end = block_end;
            builder.append(block->description());
        }

        accumulated_duration += block_duration;
    }

    builder.append(" ]"sv);

    double blocks_duration = last_block_end - first_block_start;
    double adjusted_start = (start_seconds - first_block_start) / blocks_duration;
    double adjusted_end = (end_seconds - first_block_start) / blocks_duration;

    adjusted_start = max(0.0, min(1.0, adjusted_start));
    adjusted_end = max(0.0, min(1.0, adjusted_end));

    return SelectionInfo {
        .sources = MUST(builder.to_string()),
        .adjusted_start = adjusted_start,
        .adjusted_end = adjusted_end
    };
}

ErrorOr<SampleDocument::SelectionInfo> SampleDocument::cut(double start, double end)
{
    if (start < 0.0 || end > 1.0 || start >= end)
        return Error::from_string_literal("Invalid cut range");

    auto selection = selection_info(start, end);

    double start_seconds = start * m_total_duration;
    double end_seconds = end * m_total_duration;

    Vector<NonnullRefPtr<SampleBlock>> new_blocks;
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
            double cut_start_in_block = max(0.0, start_seconds - block_start);
            double cut_end_in_block = min(block_duration, end_seconds - block_start);

            bool cut_at_start = (cut_start_in_block <= 0.001);
            bool cut_at_end = (cut_end_in_block >= block_duration - 0.001);

            if (cut_at_start && cut_at_end) {
                // Entire block is within the cut region, drop it.
            } else if (cut_at_start) {
                double fraction_to_remove = cut_end_in_block / block_duration;
                size_t samples_to_remove = static_cast<size_t>(fraction_to_remove * static_cast<double>(block->length()));

                size_t new_start = block->start() + samples_to_remove;
                size_t new_end = block->end();

                if (new_start <= new_end) {
                    auto trimmed = TRY(try_make_ref_counted<SampleBlock>(block->file(), new_start, new_end));
                    new_blocks.append(trimmed);
                }
            } else if (cut_at_end) {
                double fraction_to_keep = cut_start_in_block / block_duration;
                size_t samples_to_keep = static_cast<size_t>(fraction_to_keep * static_cast<double>(block->length()));

                size_t new_start = block->start();
                size_t new_end = block->start() + samples_to_keep - 1;

                if (new_start <= new_end) {
                    auto trimmed = TRY(try_make_ref_counted<SampleBlock>(block->file(), new_start, new_end));
                    new_blocks.append(trimmed);
                }
            } else {
                double start_fraction = cut_start_in_block / block_duration;
                double end_fraction = cut_end_in_block / block_duration;

                size_t block_length = block->end() - block->start() + 1;
                size_t cut_start_sample = static_cast<size_t>(start_fraction * static_cast<double>(block_length));
                size_t cut_end_sample = static_cast<size_t>(end_fraction * static_cast<double>(block_length));

                size_t first_start = block->start();
                size_t first_end = block->start() + cut_start_sample - 1;

                if (first_start <= first_end) {
                    auto first = TRY(try_make_ref_counted<SampleBlock>(block->file(), first_start, first_end));
                    new_blocks.append(first);
                }

                size_t second_start = block->start() + cut_end_sample;
                size_t second_end = block->end();

                if (second_start <= second_end) {
                    auto second = TRY(try_make_ref_counted<SampleBlock>(block->file(), second_start, second_end));
                    new_blocks.append(second);
                }
            }
        }

        accumulated_duration += block_duration;
    }

    m_blocks = new_blocks;
    m_total_length = calc_length();
    m_total_duration = calc_duration();
    m_render_valid = false;
    invalidate_render_lookup();

    return selection;
}

void SampleDocument::mark_render_valid() { m_render_valid = true; }

bool SampleDocument::is_render_valid() { return m_render_valid; }

bool SampleDocument::is_empty() const
{
    return m_blocks.is_empty();
}

size_t SampleDocument::length() { return m_total_length; }

double SampleDocument::duration() { return m_total_duration; }

double SampleDocument::sample_rate()
{
    if (!m_blocks.is_empty())
        return m_blocks[0]->sample_rate();
    return 44100.0;
}

ErrorOr<SampleFormat> SampleDocument::get_format()
{
    if (m_blocks.is_empty())
        return Error::from_string_literal("No blocks available");

    auto first_format = m_blocks[0]->format();

    for (size_t i = 1; i < m_blocks.size(); i++) {
        auto block_format = m_blocks[i]->format();

        if (block_format.sample_rate != first_format.sample_rate)
            return Error::from_string_literal("Blocks have inconsistent sample rates");
        if (block_format.num_channels != first_format.num_channels)
            return Error::from_string_literal("Blocks have inconsistent channel counts");
        if (block_format.bits_per_sample != first_format.bits_per_sample)
            return Error::from_string_literal("Blocks have inconsistent bit depths");
    }

    return first_format;
}

size_t SampleDocument::calc_length()
{
    size_t length = 0;
    for (auto& block : m_blocks)
        length += block->length();
    return length;
}

double SampleDocument::calc_duration()
{
    double duration = 0.0;
    for (auto& block : m_blocks)
        duration += block->duration();
    return duration;
}

RenderStruct SampleDocument::rendered_sample_at(double position)
{
    if (position < 0.0 || position > 1.0 || m_total_length == 0 || m_blocks.is_empty())
        return {};

    auto render_from_block = [&](size_t block_index, size_t total_at_end, double start, double end) {
        auto& block = m_blocks[block_index];
        double within = (position - start) / (end - start);
        size_t sample_position = static_cast<size_t>(static_cast<double>(block->length()) * within);
        m_block_lookup.valid = true;
        m_block_lookup.block = block_index;
        m_block_lookup.total_at_end = total_at_end;
        m_block_lookup.start = start;
        m_block_lookup.end = end;
        return block->rendered_sample_at(sample_position);
    };

    if (m_block_lookup.valid && m_block_lookup.block < m_blocks.size() && position >= m_block_lookup.start && position <= m_block_lookup.end)
        return render_from_block(m_block_lookup.block, m_block_lookup.total_at_end, m_block_lookup.start, m_block_lookup.end);

    size_t total = 0;
    double start = 0;
    double end = 0;
    size_t first_block = 0;

    if (m_block_lookup.valid && m_block_lookup.block < m_blocks.size() && position > m_block_lookup.end) {
        total = m_block_lookup.total_at_end;
        start = m_block_lookup.end;
        first_block = m_block_lookup.block + 1;
    }

    for (size_t block_index = first_block; block_index < m_blocks.size(); ++block_index) {
        auto& block = m_blocks[block_index];
        size_t length = block->length();
        total += length;
        end = static_cast<double>(total) / static_cast<double>(m_total_length);
        VERIFY(end > start);
        if (position <= end)
            return render_from_block(block_index, total, start, end);
        start = end;
    }

    invalidate_render_lookup();
    return {};
}

void SampleDocument::fill_render_columns(
    double viewport_start, double scale,
    size_t start_col, size_t end_col, size_t total_cols,
    Span<RenderStruct> out)
{
    if (start_col >= end_col || m_total_length == 0)
        return;
    for (auto& rs : out)
        rs = {};

    double norm_start = static_cast<double>(start_col) / (static_cast<double>(total_cols) * scale) + viewport_start;
    double norm_end = static_cast<double>(end_col) / (static_cast<double>(total_cols) * scale) + viewport_start;
    norm_start = max(0.0, min(1.0, norm_start));
    norm_end = max(0.0, min(1.0, norm_end));

    size_t container_first = static_cast<size_t>(norm_start * static_cast<double>(m_total_length));
    size_t container_last = static_cast<size_t>(norm_end * static_cast<double>(m_total_length));
    if (container_last > m_total_length)
        container_last = m_total_length;
    if (container_last <= container_first)
        return;

    size_t total_strip_samples = container_last - container_first;
    size_t num_out = end_col - start_col;

    size_t block_abs_start = 0;
    for (auto& block : m_blocks) {
        size_t block_len = block->length();
        size_t block_abs_end = block_abs_start + block_len;

        if (block_abs_end <= container_first) {
            block_abs_start = block_abs_end;
            continue;
        }
        if (block_abs_start >= container_last)
            break;

        size_t overlap_start = max(block_abs_start, container_first);
        size_t overlap_end = min(block_abs_end, container_last);
        size_t overlap_len = overlap_end - overlap_start;

        size_t strip_off_start = overlap_start - container_first;
        size_t strip_off_end = overlap_end - container_first;

        size_t col_start = (strip_off_start * num_out) / total_strip_samples;
        size_t col_end = (strip_off_end * num_out + total_strip_samples - 1) / total_strip_samples;
        if (col_end > num_out)
            col_end = num_out;

        if (col_end > col_start) {
            size_t file_first = block->start() + (overlap_start - block_abs_start);
            block->file()->fill_render_columns(
                file_first, overlap_len,
                out.slice(col_start, col_end - col_start));
        }

        block_abs_start = block_abs_end;
    }
}

void SampleDocument::begin_loading_samples()
{
    m_stream_block = 0;
    m_stream_position = 0;
    for (auto& block : m_blocks)
        block->begin_loading_samples();
}

void SampleDocument::begin_loading_samples_at(double start_position)
{
    double start_seconds = start_position * m_total_duration;
    double accumulated_duration = 0.0;

    for (size_t i = 0; i < m_blocks.size(); i++) {
        double block_duration = m_blocks[i]->duration();
        if (accumulated_duration + block_duration > start_seconds) {
            m_stream_block = i;
            m_stream_position = 0;

            for (auto& block : m_blocks)
                block->begin_loading_samples();

            return;
        }
        accumulated_duration += block_duration;
    }

    m_stream_block = !m_blocks.is_empty() ? m_blocks.size() - 1 : 0;
    m_stream_position = 0;
    for (auto& block : m_blocks)
        block->begin_loading_samples();
}

FixedArray<Audio::Sample> SampleDocument::load_more_samples()
{
    size_t num_blocks = m_blocks.size();
    if (m_stream_block < num_blocks) {
        auto& block = m_blocks[m_stream_block];
        auto samples = block->load_more_samples();
        if (samples.is_empty()) {
            m_stream_block++;
            m_stream_position = 0;
            return load_more_samples();
        }
        return samples;
    }
    return SampleBuffer::null_samples();
}

FixedArray<Audio::Sample> SampleDocument::load_more_samples_in_range(double start, double end, size_t& samples_loaded)
{
    if (m_blocks.is_empty() || m_total_length == 0)
        return SampleBuffer::null_samples();

    size_t start_sample = static_cast<size_t>(start * static_cast<double>(m_total_length));
    size_t end_sample = static_cast<size_t>(end * static_cast<double>(m_total_length));
    size_t total_samples_in_range = end_sample - start_sample;

    if (samples_loaded >= total_samples_in_range)
        return SampleBuffer::null_samples();

    if (m_stream_block >= m_blocks.size())
        return SampleBuffer::null_samples();

    auto& block = m_blocks[m_stream_block];
    auto samples = block->load_more_samples();

    if (samples.is_empty()) {
        m_stream_block++;
        m_stream_position = 0;
        return load_more_samples_in_range(start, end, samples_loaded);
    }

    size_t current_absolute_position = 0;
    for (size_t i = 0; i < m_stream_block; i++)
        current_absolute_position += m_blocks[i]->length();
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

    if (samples_to_use == 0 && samples_remaining > 0)
        return load_more_samples_in_range(start, end, samples_loaded);

    if (skip_samples > 0 || samples_to_use < samples.size()) {
        auto trimmed = FixedArray<Audio::Sample>::create(samples_to_use);
        if (trimmed.is_error())
            return SampleBuffer::null_samples();

        auto result = trimmed.release_value();
        for (size_t i = 0; i < samples_to_use; i++)
            result[i] = samples[skip_samples + i];
        samples_loaded += samples_to_use;
        return result;
    }

    samples_loaded += samples.size();
    return samples;
}
