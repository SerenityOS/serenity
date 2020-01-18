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

#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GWidget.h>
#include <stdio.h>

//#define GBOXLAYOUT_DEBUG

GBoxLayout::GBoxLayout(Orientation orientation)
    : m_orientation(orientation)
{
}

GBoxLayout::~GBoxLayout()
{
}

void GBoxLayout::run(GWidget& widget)
{
    bool should_log = false;
#ifdef GBOXLAYOUT_DEBUG
    should_log = true;
#endif
    if (should_log)
        dbgprintf("GBoxLayout: running layout on %s{%p}, entry count: %d\n", widget.class_name(), &widget, m_entries.size());

    if (m_entries.is_empty())
        return;

    Size available_size = widget.size();
    int number_of_entries_with_fixed_size = 0;

    int number_of_visible_entries = 0;

    if (should_log)
        dbgprintf("GBoxLayout:  Starting with available size: %s\n", available_size.to_string().characters());

    for (auto& entry : m_entries) {
        if (entry.type == Entry::Type::Spacer) {
            ++number_of_visible_entries;
        }
        if (!entry.widget)
            continue;

        if (!entry.widget->is_visible())
            continue;
        ++number_of_visible_entries;
        if (entry.widget && entry.widget->size_policy(orientation()) == SizePolicy::Fixed) {
            if (should_log) {
                dbgprintf("GBoxLayout:   Subtracting for fixed %s{%p}, size: %s\n", entry.widget->class_name(), entry.widget.ptr(), entry.widget->preferred_size().to_string().characters());
                dbgprintf("GBoxLayout:     Available size before: %s\n", available_size.to_string().characters());
            }
            available_size -= entry.widget->preferred_size();
            if (should_log)
                dbgprintf("GBoxLayout:     Available size  after: %s\n", available_size.to_string().characters());
            ++number_of_entries_with_fixed_size;
        }
        available_size -= { spacing(), spacing() };
    }

    available_size += { spacing(), spacing() };

    available_size -= { margins().left() + margins().right(), margins().top() + margins().bottom() };

    if (should_log)
        dbgprintf("GBoxLayout:  Number of visible: %d/%d\n", number_of_visible_entries, m_entries.size());

    int number_of_entries_with_automatic_size = number_of_visible_entries - number_of_entries_with_fixed_size;

    if (should_log)
        dbgprintf("GBoxLayout:   available_size=%s, fixed=%d, fill=%d\n", available_size.to_string().characters(), number_of_entries_with_fixed_size, number_of_entries_with_automatic_size);

    Size automatic_size;

    if (number_of_entries_with_automatic_size) {
        if (m_orientation == Orientation::Horizontal) {
            automatic_size.set_width(available_size.width() / number_of_entries_with_automatic_size);
            automatic_size.set_height(widget.height());
        } else {
            automatic_size.set_width(widget.width());
            automatic_size.set_height(available_size.height() / number_of_entries_with_automatic_size);
        }
    }

    if (should_log)
        dbgprintf("GBoxLayout:   automatic_size=%s\n", automatic_size.to_string().characters());

    int current_x = margins().left();
    int current_y = margins().top();

    for (auto& entry : m_entries) {
        if (entry.type == Entry::Type::Spacer) {
            current_x += automatic_size.width();
            current_y += automatic_size.height();
        }

        if (!entry.widget)
            continue;
        if (!entry.widget->is_visible())
            continue;
        Rect rect(current_x, current_y, 0, 0);
        if (entry.layout) {
            // FIXME: Implement recursive layout.
            ASSERT_NOT_REACHED();
        }
        ASSERT(entry.widget);
        rect.set_size(automatic_size);

        if (entry.widget->size_policy(Orientation::Vertical) == SizePolicy::Fixed)
            rect.set_height(entry.widget->preferred_size().height());

        if (entry.widget->size_policy(Orientation::Horizontal) == SizePolicy::Fixed)
            rect.set_width(entry.widget->preferred_size().width());

        if (orientation() == Orientation::Horizontal) {
            if (entry.widget->size_policy(Orientation::Vertical) == SizePolicy::Fill)
                rect.set_height(widget.height() - margins().top() - margins().bottom());
            rect.center_vertically_within(widget.rect());
        } else {
            if (entry.widget->size_policy(Orientation::Horizontal) == SizePolicy::Fill)
                rect.set_width(widget.width() - margins().left() - margins().right());
            rect.center_horizontally_within(widget.rect());
        }

        if (should_log)
            dbgprintf("GBoxLayout: apply, %s{%p} <- %s\n", entry.widget->class_name(), entry.widget.ptr(), rect.to_string().characters());
        entry.widget->set_relative_rect(rect);

        if (orientation() == Orientation::Horizontal)
            current_x += rect.width() + spacing();
        else
            current_y += rect.height() + spacing();
    }
}
