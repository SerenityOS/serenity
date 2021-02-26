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

namespace VT {

Line::Line(size_t length)
{
    set_length(length);
}

Line::~Line()
{
}

void Line::set_length(size_t new_length)
{
    size_t old_length = length();
    if (old_length == new_length)
        return;
    m_cells.resize(new_length);
}

void Line::clear(const Attribute& attribute)
{
    if (m_dirty) {
        for (auto& cell : m_cells) {
            cell = Cell { .code_point = ' ', .attribute = attribute };
        }
        return;
    }
    for (auto& cell : m_cells) {
        if (!m_dirty)
            m_dirty = cell.code_point != ' ' || cell.attribute != attribute;
        cell = Cell { .code_point = ' ', .attribute = attribute };
    }
}

bool Line::has_only_one_background_color() const
{
    if (!length())
        return true;
    // FIXME: Cache this result?
    auto color = attribute_at(0).effective_background_color();
    for (size_t i = 1; i < length(); ++i) {
        if (attribute_at(i).effective_background_color() != color)
            return false;
    }
    return true;
}

}
