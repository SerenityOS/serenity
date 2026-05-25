/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleBlock.h"
#include "SampleBuffer.h"
#include "SampleFormatStruct.h"
#include <AK/ByteString.h>
#include <AK/Optional.h>
#include <AK/RefPtr.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibAudio/Sample.h>

class SampleDocument {
public:
    SampleDocument();
    ErrorOr<double> parse_and_insert(String json, double position);
    void set(NonnullRefPtr<SampleBlock> block);
    void append(NonnullRefPtr<SampleBlock> block);
    void clear();
    size_t length();
    double duration();
    double sample_rate();
    ErrorOr<SampleFormat> get_format();
    RenderStruct rendered_sample_at(double position);
    void fill_render_columns(double viewport_start, double scale, size_t start_col, size_t end_col, size_t total_cols, Span<RenderStruct> out);
    bool is_empty() const;
    bool is_render_valid();
    void mark_render_valid();
    void begin_loading_samples();
    void begin_loading_samples_at(double start_position);
    FixedArray<Audio::Sample> load_more_samples();
    FixedArray<Audio::Sample> load_more_samples_in_range(double start, double end, size_t& samples_loaded);
    String sources();
    String sources_for_range(double start, double end);
    struct SelectionInfo {
        String sources;
        double adjusted_start;
        double adjusted_end;
    };
    SelectionInfo selection_info(double start, double end);
    ErrorOr<SelectionInfo> cut(double start, double end);

private:
    struct SourceInfo {
        ByteString path;
        i64 length { 0 };
        i64 file_start { 0 };
        i64 file_end { 0 };
        i64 rate { 44100 };
        i64 channels { 1 };
        i64 bits { 16 };
        double duration { 0.0 };
    };
    struct ClipboardData {
        double start { 0.0 };
        double end { 0.0 };
        Vector<SourceInfo> sources;
        double total_duration { 0.0 };
    };
    struct BlockLookupCache {
        bool valid { false };
        size_t block { 0 };
        size_t total_at_end { 0 };
        double start { 0.0 };
        double end { 0.0 };
    };

    static ErrorOr<ClipboardData> parse_clipboard_json(String const&);
    ErrorOr<Vector<NonnullRefPtr<SampleBlock>>> load_blocks_from_clipboard(ClipboardData const&, size_t start_block_index, size_t end_block_index, double start_point, double end_point);
    ErrorOr<void> splice_blocks(Vector<NonnullRefPtr<SampleBlock>> const&, Optional<size_t> position_block_index, double position_point);
    void invalidate_render_lookup();
    size_t calc_length();
    double calc_duration();

    AK::Vector<NonnullRefPtr<SampleBlock>> m_blocks;
    size_t m_total_length { 0 };
    double m_total_duration { 0.0 };
    bool m_render_valid { false };
    size_t m_stream_position { 0 };
    size_t m_stream_block { 0 };
    BlockLookupCache m_block_lookup;
};
