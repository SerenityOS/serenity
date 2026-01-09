/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleBuffer.h"
#include "SampleFormatStruct.h"
#include <AK/FixedArray.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibAudio/Sample.h>

class SampleSourceFile : public RefCounted<SampleSourceFile> {

public:
    SampleSourceFile(StringView filename);
    RenderStruct rendered_sample_at(size_t position);
    size_t length();
    double duration();
    double sample_rate();
    String filename();
    ErrorOr<NonnullRefPtr<SampleSourceFile>> try_create(StringView filename);
    void begin_loading_samples();
    FixedArray<Audio::Sample> load_more_samples();
    SampleFormat format() { return m_format; }

private:
    void load_metadata();
    RenderStruct rendered_sample_within_buffer(FixedArray<Audio::Sample>& buffer, size_t at);
    String m_filename;
    FixedArray<Audio::Sample> m_buffer;
    StringView m_format_name;
    size_t m_samples;
    SampleFormat m_format;
    size_t m_buffer_position { 0 };
    bool m_buffered { false };
    bool m_loading { false };
    size_t m_stream_position { 0 };
};
