/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RenderStruct.h"
#include "SampleBlockContainer.h"
#include "SampleFormatStruct.h"
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>

class SampleRenderer : public RefCounted<SampleRenderer> {

public:
    SampleRenderer(SampleBlockContainer& samples, size_t width, double start, double scale, size_t start_offset, size_t end_offset);
    RenderStruct rendered_sample_at(size_t index);

private:
    size_t m_width { 0 };
    Vector<RenderStruct> m_buffer;
};
