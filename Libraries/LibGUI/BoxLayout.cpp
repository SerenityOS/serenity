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

#include <AK/JsonObject.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Orientation.h>
#include <stdio.h>

//#define GBOXLAYOUT_DEBUG

namespace GUI {

BoxLayout::BoxLayout(Orientation orientation)
    : m_orientation(orientation)
{
    register_property(
        "orientation", [this] { return m_orientation == Gfx::Orientation::Vertical ? "Vertical" : "Horizontal"; }, nullptr);
}

void BoxLayout::run(Widget& widget)
{
    bool should_log = false;
#ifdef GBOXLAYOUT_DEBUG
    should_log = true;
#endif
    if (should_log)
        dbgprintf("BoxLayout: running layout on %s{%p}, entry count: %zu\n", widget.class_name(), &widget, m_entries.size());

    if (m_entries.is_empty())
        return;

    Gfx::IntSize available_size = widget.size();
    int number_of_entries_with_fixed_size = 0;

    int number_of_visible_entries = 0;

    if (should_log)
        dbgprintf("BoxLayout:  Starting with available size: %s\n", available_size.to_string().characters());

    for (size_t i = 0; i < m_entries.size(); ++i) {
        auto& entry = m_entries[i];
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
                dbgprintf("BoxLayout:   Subtracting for fixed %s{%p}, size: %s\n", entry.widget->class_name(), entry.widget.ptr(), entry.widget->preferred_size().to_string().characters());
                dbgprintf("BoxLayout:     Available size before: %s\n", available_size.to_string().characters());
            }
            available_size -= entry.widget->preferred_size();
            if (should_log)
                dbgprintf("BoxLayout:     Available size  after: %s\n", available_size.to_string().characters());
            ++number_of_entries_with_fixed_size;
        }
        available_size -= { spacing(), spacing() };
    }

    available_size += { spacing(), spacing() };

    available_size -= { margins().left() + margins().right(), margins().top() + margins().bottom() };

    if (should_log)
        dbgprintf("BoxLayout:  Number of visible: %d/%zu\n", number_of_visible_entries, m_entries.size());

    int number_of_entries_with_automatic_size = number_of_visible_entries - number_of_entries_with_fixed_size;

    if (should_log)
        dbgprintf("BoxLayout:   available_size=%s, fixed=%d, fill=%d\n", available_size.to_string().characters(), number_of_entries_with_fixed_size, number_of_entries_with_automatic_size);

    Gfx::IntSize automatic_size;

    int remaining_size = 0;
    int number_of_entries_with_automatic_size_remaining = number_of_entries_with_automatic_size;

    if (number_of_entries_with_automatic_size) {
        if (m_orientation == Orientation::Horizontal) {
            automatic_size.set_width(available_size.width() / number_of_entries_with_automatic_size);
            automatic_size.set_height(widget.height());

            remaining_size = available_size.width();
        } else {
            automatic_size.set_width(widget.width());
            automatic_size.set_height(available_size.height() / number_of_entries_with_automatic_size);

            remaining_size = available_size.height();
        }
    }

    if (should_log)
        dbgprintf("BoxLayout:   automatic_size=%s\n", automatic_size.to_string().characters());

    int current_x = margins().left();
    int current_y = margins().top();

    for (size_t i = 0; i < m_entries.size(); ++i) {
        auto& entry = m_entries[i];
        if (entry.type == Entry::Type::Spacer) {
            current_x += automatic_size.width();
            current_y += automatic_size.height();
            continue;
        }

        if (!entry.widget)
            continue;
        if (!entry.widget->is_visible())
            continue;
        Gfx::IntRect rect(current_x, current_y, 0, 0);
        if (entry.layout) {
            // FIXME: Implement recursive layout.
            ASSERT_NOT_REACHED();
        }
        ASSERT(entry.widget);

        if (entry.widget->size_policy(Orientation::Vertical) == SizePolicy::Fixed) {
            rect.set_width(widget.width());
            rect.set_height(entry.widget->preferred_size().height());
        } else {
            if (orientation() == Orientation::Horizontal)
                rect.set_height(widget.height());
            else
                rect.set_height(remaining_size / number_of_entries_with_automatic_size_remaining);
        }

        if (entry.widget->size_policy(Orientation::Horizontal) == SizePolicy::Fixed) {
            rect.set_width(entry.widget->preferred_size().width());
            rect.set_height(widget.height());
        } else {
            if (orientation() == Orientation::Horizontal)
                rect.set_width(remaining_size / number_of_entries_with_automatic_size_remaining);
            else
                rect.set_width(widget.width());
        }

        if (orientation() == Orientation::Horizontal) {
            if (entry.widget->size_policy(Orientation::Vertical) == SizePolicy::Fill)
                rect.set_height(widget.height() - margins().top() - margins().bottom());
        } else {
            if (entry.widget->size_policy(Orientation::Horizontal) == SizePolicy::Fill)
                rect.set_width(widget.width() - margins().left() - margins().right());
        }

        // Apply min/max constraints to filled widgets.
        if (entry.widget->size_policy(Orientation::Horizontal) == SizePolicy::Fill) {
            if (entry.widget->min_size().width() >= 0)
                rect.set_width(max(entry.widget->min_size().width(), rect.width()));
            if (entry.widget->max_size().width() >= 0)
                rect.set_width(min(entry.widget->max_size().width(), rect.width()));
        }
        if (entry.widget->size_policy(Orientation::Vertical) == SizePolicy::Fill) {
            if (entry.widget->min_size().height() >= 0)
                rect.set_height(max(entry.widget->min_size().height(), rect.height()));
            if (entry.widget->max_size().height() >= 0)
                rect.set_height(min(entry.widget->max_size().height(), rect.height()));
        }

        if (orientation() == Orientation::Horizontal)
            rect.center_vertically_within(widget.rect());
        else
            rect.center_horizontally_within(widget.rect());

        if (should_log)
            dbgprintf("BoxLayout: apply, %s{%p} <- %s\n", entry.widget->class_name(), entry.widget.ptr(), rect.to_string().characters());

        entry.widget->set_relative_rect(rect);

        if (orientation() == Orientation::Horizontal) {
            if (entry.widget->size_policy(Orientation::Horizontal) == SizePolicy::Fill) {
                remaining_size -= rect.width();
                --number_of_entries_with_automatic_size_remaining;
            }
            current_x += rect.width() + spacing();
        } else {
            if (entry.widget->size_policy(Orientation::Vertical) == SizePolicy::Fill) {
                remaining_size -= rect.height();
                --number_of_entries_with_automatic_size_remaining;
            }
            current_y += rect.height() + spacing();
        }
    }
}
}
