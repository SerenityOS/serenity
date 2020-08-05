/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibVT/Line.h>
#include <string.h>

namespace VT {

Line::Line(u16 length)
{
    set_length(length);
}

Line::~Line()
{
    if (m_utf32)
        delete[] m_code_points.as_u32;
    else
        delete[] m_code_points.as_u8;
    delete[] m_attributes;
}

template<typename CodepointType>
static CodepointType* create_new_code_point_array(size_t new_length, const CodepointType* old_code_points, size_t old_length)
{
    auto* new_code_points = new CodepointType[new_length];
    for (size_t i = 0; i < new_length; ++i)
        new_code_points[i] = ' ';
    if (old_code_points) {
        for (size_t i = 0; i < min(old_length, new_length); ++i) {
            new_code_points[i] = old_code_points[i];
        }
    }
    delete[] old_code_points;
    return new_code_points;
}

void Line::set_length(u16 new_length)
{
    if (m_length == new_length)
        return;

    if (m_utf32)
        m_code_points.as_u32 = create_new_code_point_array<u32>(new_length, m_code_points.as_u32, m_length);
    else
        m_code_points.as_u8 = create_new_code_point_array<u8>(new_length, m_code_points.as_u8, m_length);

    auto* new_attributes = new Attribute[new_length];
    if (m_attributes) {
        for (size_t i = 0; i < min(m_length, new_length); ++i)
            new_attributes[i] = m_attributes[i];
    }
    delete[] m_attributes;
    m_attributes = new_attributes;
    m_length = new_length;
}

void Line::clear(Attribute attribute)
{
    if (m_dirty) {
        for (u16 i = 0; i < m_length; ++i) {
            set_code_point(i, ' ');
            m_attributes[i] = attribute;
        }
        return;
    }
    for (unsigned i = 0; i < m_length; ++i) {
        if (code_point(i) != ' ')
            m_dirty = true;
        set_code_point(i, ' ');
    }
    for (unsigned i = 0; i < m_length; ++i) {
        if (m_attributes[i] != attribute)
            m_dirty = true;
        m_attributes[i] = attribute;
    }
}

bool Line::has_only_one_background_color() const
{
    if (!m_length)
        return true;
    // FIXME: Cache this result?
    auto color = m_attributes[0].background_color;
    for (size_t i = 1; i < m_length; ++i) {
        if (m_attributes[i].background_color != color)
            return false;
    }
    return true;
}

void Line::convert_to_utf32()
{
    ASSERT(!m_utf32);
    auto* new_code_points = new u32[m_length];
    for (size_t i = 0; i < m_length; ++i) {
        new_code_points[i] = m_code_points.as_u8[i];
    }
    delete m_code_points.as_u8;
    m_code_points.as_u32 = new_code_points;
    m_utf32 = true;
}

}
