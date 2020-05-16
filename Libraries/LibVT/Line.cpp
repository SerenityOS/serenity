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
    delete[] m_codepoints;
    delete[] m_attributes;
}

void Line::set_length(u16 new_length)
{
    if (m_length == new_length)
        return;
    auto* new_codepoints = new u32[new_length];
    auto* new_attributes = new Attribute[new_length];
    for (size_t i = 0; i < new_length; ++i)
        new_codepoints[i] = ' ';
    if (m_codepoints && m_attributes) {
        for (size_t i = 0; i < min(m_length, new_length); ++i) {
            new_codepoints[i] = m_codepoints[i];
            new_attributes[i] = m_attributes[i];
        }
    }
    delete[] m_codepoints;
    delete[] m_attributes;
    m_codepoints = new_codepoints;
    m_attributes = new_attributes;
    m_length = new_length;
}

void Line::clear(Attribute attribute)
{
    if (m_dirty) {
        for (u16 i = 0; i < m_length; ++i) {
            m_codepoints[i] = ' ';
            m_attributes[i] = attribute;
        }
        return;
    }
    for (unsigned i = 0; i < m_length; ++i) {
        if (m_codepoints[i] != ' ')
            m_dirty = true;
        m_codepoints[i] = ' ';
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

}
