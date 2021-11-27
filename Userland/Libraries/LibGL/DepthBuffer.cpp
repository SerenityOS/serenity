/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DepthBuffer.h"

namespace GL {

DepthBuffer::DepthBuffer(Gfx::IntSize const& size)
    : m_size(size)
    , m_data(new float[size.width() * size.height()])
{
}

DepthBuffer::~DepthBuffer()
{
    delete[] m_data;
}

float* DepthBuffer::scanline(int y)
{
    VERIFY(y >= 0 && y < m_size.height());
    return &m_data[y * m_size.width()];
}

void DepthBuffer::clear(float depth)
{
    int num_entries = m_size.width() * m_size.height();
    for (int i = 0; i < num_entries; ++i) {
        m_data[i] = depth;
    }
}

void DepthBuffer::clear(Gfx::IntRect bounds, float depth)
{
    bounds.intersect({ 0, 0, m_size.width(), m_size.height() });
    for (int y = bounds.top(); y <= bounds.bottom(); ++y)
        for (int x = bounds.left(); x <= bounds.right(); ++x)
            m_data[y * m_size.width() + x] = depth;
}

}
