/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleBuffer.h"
#include "SampleFormatStruct.h"
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibAudio/Sample.h>

class SampleBlock : public RefCounted<SampleBlock> {

public:
    // Factory method for creating a null (silence) block
    static NonnullRefPtr<SampleBlock> create_null(size_t size, double duration);

    virtual size_t length();
    virtual double duration();
    virtual double sample_rate();
    virtual String description();

    virtual ~SampleBlock() = default;
    RenderStruct rendered_sample_at(size_t position)
    {
        return ((position < length()) ? rendered_sample_at_valid(position) : (RenderStruct) { 0, 0, 0, 0 });
    }
    virtual void begin_loading_samples();
    virtual FixedArray<Audio::Sample> load_more_samples();
    SampleFormat format() { return m_format; }

protected:
    SampleBlock() = default;
    explicit SampleBlock(size_t size, double duration);
    virtual RenderStruct rendered_sample_at_valid(size_t position);
    SampleFormat m_format;

private:
    size_t m_size { 0 };
    double m_duration { 0.0 };
    size_t m_position { 0 };
};
