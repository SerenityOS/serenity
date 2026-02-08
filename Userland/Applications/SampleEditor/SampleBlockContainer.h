/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleBlock.h"
#include "SampleBuffer.h"
#include "SampleFormatStruct.h"
#include <AK/ByteString.h>
#include <AK/RefPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibAudio/Sample.h>

class SampleBlockContainer {

public:
    SampleBlockContainer();
    void set(NonnullRefPtr<SampleBlock> block);
    void append(NonnullRefPtr<SampleBlock> block);
    size_t length();
    double duration();
    double sample_rate();
    ErrorOr<SampleFormat> get_format();
    RenderStruct rendered_sample_at(double position);
    bool used();
    void set_used();
    void begin_loading_samples();
    void begin_loading_samples_at(double start_position);
    FixedArray<Audio::Sample> load_more_samples();
    FixedArray<Audio::Sample> load_more_samples_in_range(double start, double end, size_t& samples_loaded);

private:
    AK::Vector<NonnullRefPtr<SampleBlock>> m_blocks;
    size_t calc_length();
    double calc_duration();
    size_t m_total_length { 0 };
    double m_total_duration { 0.0 };
    bool m_used { false };
    size_t m_stream_position { 0 };
    size_t m_stream_block { 0 };
};
