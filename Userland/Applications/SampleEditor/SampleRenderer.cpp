/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleRenderer.h"

#include <AK/Types.h>

#include "SampleFormatStruct.h"

SampleRenderer::SampleRenderer(SampleBlockContainer& samples, size_t width,
    double start, double scale, size_t start_offset, size_t end_offset)
{
    m_width = width;
    m_buffer = Vector<RenderStruct>();

    for (size_t i = 0; i < m_width; i++) {
        RenderStruct value;
        if (i >= start_offset && i < end_offset) {
            double position = ((double)i / (double)m_width) / scale + start;
            value = samples.rendered_sample_at(position);
        }
        m_buffer.append(value);
    }
}

RenderStruct SampleRenderer::rendered_sample_at(size_t index)
{
    auto render_value = m_buffer[index];
    return render_value;
}
