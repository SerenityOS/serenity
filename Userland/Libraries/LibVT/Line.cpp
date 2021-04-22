/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
