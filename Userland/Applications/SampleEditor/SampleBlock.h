/*
 * Copyright (c) 2026, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleFile.h"
#include "SampleFormatStruct.h"
#include <AK/Array.h>
#include <AK/FixedArray.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibAudio/Sample.h>

class SampleBlock : public RefCounted<SampleBlock> {
public:
    SampleBlock(NonnullRefPtr<SampleFile> file, size_t start, size_t end);

    size_t length();
    double duration();
    double sample_rate();
    String description();
    SampleFormat format() { return m_format; }
    NonnullRefPtr<SampleFile> file() { return m_file; }
    size_t start() const { return m_start; }
    size_t end() const { return m_end; }

    RenderStruct rendered_sample_at(size_t position);
    void begin_loading_samples();
    FixedArray<Audio::Sample> load_more_samples();

    ErrorOr<Array<NonnullRefPtr<SampleBlock>, 2>> split_at(double position);

private:
    NonnullRefPtr<SampleFile> m_file;
    size_t m_start { 0 };
    size_t m_end { 0 };
    size_t m_length { 0 };
    double m_duration { 0.0 };
    size_t m_stream_position { 0 };
    RefPtr<Audio::Loader> m_stream_loader;
    SampleFormat m_format;
};
