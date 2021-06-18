/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/JsonObject.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Widget.h>

namespace GUI {

Layout::Layout()
{
    REGISTER_INT_PROPERTY("spacing", spacing, set_spacing);
    REGISTER_MARGINS_PROPERTY("margins", margins, set_margins);

    register_property("entries",
        [this] {
            JsonArray entries_array;
            for (auto& entry : m_entries) {
                JsonObject entry_object;
                if (entry.type == Entry::Type::Widget) {
                    entry_object.set("type", "Widget");
                    entry_object.set("widget", (FlatPtr)entry.widget.ptr());
                } else if (entry.type == Entry::Type::Spacer) {
                    entry_object.set("type", "Spacer");
                } else {
                    VERIFY_NOT_REACHED();
                }
                entries_array.append(move(entry_object));
            }
            return entries_array;
        });
}

Layout::~Layout()
{
}

void Layout::notify_adopted(Badge<Widget>, Widget& widget)
{
    if (m_owner == &widget)
        return;
    m_owner = widget;
}

void Layout::notify_disowned(Badge<Widget>, Widget& widget)
{
    VERIFY(m_owner == &widget);
    m_owner.clear();
}

void Layout::add_entry(Entry&& entry)
{
    m_entries.append(move(entry));
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void Layout::add_spacer()
{
    Entry entry;
    entry.type = Entry::Type::Spacer;
    add_entry(move(entry));
}

void Layout::add_layout(OwnPtr<Layout>&& layout)
{
    Entry entry;
    entry.type = Entry::Type::Layout;
    entry.layout = move(layout);
    add_entry(move(entry));
}

void Layout::add_widget(Widget& widget)
{
    Entry entry;
    entry.type = Entry::Type::Widget;
    entry.widget = widget;
    add_entry(move(entry));
}

void Layout::insert_widget_before(Widget& widget, Widget& before_widget)
{
    Entry entry;
    entry.type = Entry::Type::Widget;
    entry.widget = widget;
    m_entries.insert_before_matching(move(entry), [&](auto& existing_entry) {
        return existing_entry.type == Entry::Type::Widget && existing_entry.widget.ptr() == &before_widget;
    });
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void Layout::remove_widget(Widget& widget)
{
    m_entries.remove_first_matching([&](auto& entry) {
        return entry.widget == &widget;
    });
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void Layout::set_spacing(int spacing)
{
    if (m_spacing == spacing)
        return;
    m_spacing = spacing;
    if (m_owner)
        m_owner->notify_layout_changed({});
}

void Layout::set_margins(const Margins& margins)
{
    if (m_margins == margins)
        return;
    m_margins = margins;
    if (m_owner)
        m_owner->notify_layout_changed({});
}

}
