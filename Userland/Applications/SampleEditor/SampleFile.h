/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleFormatStruct.h"
#include <AK/FixedArray.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Sample.h>

class SampleFile : public RefCounted<SampleFile> {
public:
    SampleFile(StringView filename);

    size_t length();
    double duration();
    double sample_rate();
    String filename();
    SampleFormat format() { return m_format; }

    RenderStruct rendered_sample_at(size_t position);
    void fill_render_columns(size_t first_sample, size_t total_samples, Span<RenderStruct> out);

    void begin_loading_samples();
    FixedArray<Audio::Sample> load_more_samples();

private:
    void load_metadata();
    RefPtr<Audio::Loader> ensure_render_loader();
    RenderStruct rendered_sample_within_buffer(FixedArray<Audio::Sample>& buffer, size_t at);

    String m_filename;
    FixedArray<Audio::Sample> m_buffer;
    RefPtr<Audio::Loader> m_render_loader;
    RefPtr<Audio::Loader> m_stream_loader;
    SampleFormat m_format;
    size_t m_samples { 0 };
    size_t m_buffer_position { 0 };
    bool m_buffered { false };
    size_t m_stream_position { 0 };
};
