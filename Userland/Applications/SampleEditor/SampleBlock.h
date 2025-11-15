/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleBuffer.h"
#include "SampleFormatStruct.h"
#include "SampleSourceFile.h"
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibAudio/Sample.h>

class SampleBlock : public RefCounted<SampleBlock> {

public:
    virtual size_t length() = 0;
    virtual double duration() = 0;
    virtual double sample_rate() = 0;
    virtual String description() = 0;

    virtual ~SampleBlock() = default;
    RenderStruct rendered_sample_at(size_t position)
    {
        return ((position < length()) ? rendered_sample_at_valid(position) : (RenderStruct) { 0, 0, 0, 0 });
    }
    virtual void begin_loading_samples() = 0;
    virtual FixedArray<Audio::Sample> load_more_samples() = 0;
    SampleFormat format() { return m_format; }

protected:
    virtual RenderStruct rendered_sample_at_valid(size_t position) = 0;
    SampleFormat m_format;

private:
};
