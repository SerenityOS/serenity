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
#include "SampleSourceFile.h"
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibAudio/Sample.h>

class SampleFileBlock : public SampleBlock {

public:
    SampleFileBlock(NonnullRefPtr<SampleSourceFile> file, size_t start, size_t end);
    size_t length() override;
    double duration() override;
    double sample_rate() override;
    String description() override;
    void begin_loading_samples() override;
    FixedArray<Audio::Sample> load_more_samples() override;

    // Split this block at a fractional position (0.0 to 1.0)
    // Returns a pair: (first_part, second_part)
    ErrorOr<Array<NonnullRefPtr<SampleFileBlock>, 2>> split_at(double position);

    // Accessors for block range within file
    size_t start() const { return m_start; }
    size_t end() const { return m_end; }
    NonnullRefPtr<SampleSourceFile> file() const { return m_file; }

protected:
    NonnullRefPtr<SampleSourceFile> m_file;
    size_t m_start { 0 };
    size_t m_end { 0 };
    size_t m_length { 0 };
    double m_duration { 0.0 };
    size_t m_stream_position { 0 }; // Track position for streaming playback

    RenderStruct rendered_sample_at_valid(size_t position) override;

private:
};
