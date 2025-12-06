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
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibAudio/Sample.h>

class SampleNullBlock : public SampleBlock {

public:
    SampleNullBlock(size_t size, double duration);
    size_t length() override;
    double duration() override;
    double sample_rate() override;
    String description() override;
    void begin_loading_samples() override;
    FixedArray<Audio::Sample> load_more_samples() override;

protected:
    size_t m_size { 0 };
    double m_duration { 0 };
    RenderStruct rendered_sample_at_valid(size_t position) override;

private:
    size_t m_position { 0 };
};
