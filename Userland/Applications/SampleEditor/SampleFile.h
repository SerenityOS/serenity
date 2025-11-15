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
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibAudio/Sample.h>

class SampleFile : public RefCounted<SampleFile> {

public:
    virtual size_t length() = 0;
    virtual double duration() = 0;
    virtual double sample_rate() = 0;
    virtual ~SampleFile() = default;
    virtual RenderStruct rendered_sample_at(size_t position) = 0;
    virtual void begin_loading_samples() = 0;
    virtual FixedArray<Audio::Sample> load_more_samples() = 0;
    virtual String filename() = 0;
    SampleFormat format() { return m_format; }

protected:
    SampleFormat m_format;
};
