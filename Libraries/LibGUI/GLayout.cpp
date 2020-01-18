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

#include <LibGUI/GLayout.h>
#include <LibGUI/GWidget.h>

GLayout::GLayout()
{
}

GLayout::~GLayout()
{
}

void GLayout::notify_adopted(Badge<GWidget>, GWidget& widget)
{
    if (m_owner == &widget)
        return;
    m_owner = widget.make_weak_ptr();
}

void GLayout::notify_disowned(Badge<GWidget>, GWidget& widget)
{
    ASSERT(m_owner == &widget);
    m_owner.clear();
}

void GLayout::add_entry(Entry&& entry)
{
    m_entries.append(move(entry));
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void GLayout::add_spacer()
{
    Entry entry;
    entry.type = Entry::Type::Spacer;
    add_entry(move(entry));
}

void GLayout::add_layout(OwnPtr<GLayout>&& layout)
{
    Entry entry;
    entry.type = Entry::Type::Layout;
    entry.layout = move(layout);
    add_entry(move(entry));
}

void GLayout::add_widget(GWidget& widget)
{
    Entry entry;
    entry.type = Entry::Type::Widget;
    entry.widget = widget.make_weak_ptr();
    add_entry(move(entry));
}

void GLayout::insert_widget_before(GWidget& widget, GWidget& before_widget)
{
    Entry entry;
    entry.type = Entry::Type::Widget;
    entry.widget = widget.make_weak_ptr();
    m_entries.insert_before_matching(move(entry), [&](auto& existing_entry) {
        return existing_entry.type == Entry::Type::Widget && existing_entry.widget.ptr() == &before_widget;
    });
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void GLayout::remove_widget(GWidget& widget)
{
    m_entries.remove_first_matching([&](auto& entry) {
        return entry.widget == &widget;
    });
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void GLayout::set_spacing(int spacing)
{
    if (m_spacing == spacing)
        return;
    m_spacing = spacing;
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void GLayout::set_margins(const GMargins& margins)
{
    if (m_margins == margins)
        return;
    m_margins = margins;
    if (m_owner)
        m_owner->notify_layout_changed({});
}
