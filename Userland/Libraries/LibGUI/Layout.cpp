/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/JsonObject.h>
#include <LibGUI/Layout.h>
#include <LibGUI/Widget.h>

REGISTER_ABSTRACT_CORE_OBJECT(GUI, Layout)

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
    m_owner->for_each_child_widget([&](Widget& child) {
        add_widget(child);
        return IterationDecision::Continue;
    });
}

void Layout::notify_disowned(Badge<Widget>, Widget& widget)
{
    VERIFY(m_owner == &widget);
    m_owner.clear();
    m_entries.clear();
}

ErrorOr<void> Layout::try_add_entry(Entry&& entry)
{
    TRY(m_entries.try_append(move(entry)));
    if (m_owner)
        m_owner->notify_layout_changed({});
    return {};
}

void Layout::add_entry(Entry&& entry)
{
    MUST(try_add_entry(move(entry)));
}

ErrorOr<void> Layout::try_add_spacer()
{
    TRY(try_add_entry(Entry { .type = Entry::Type::Spacer }));
    return {};
}

void Layout::add_spacer()
{
    MUST(try_add_spacer());
}

void Layout::add_layout(OwnPtr<Layout>&& layout)
{
    Entry entry;
    entry.type = Entry::Type::Layout;
    entry.layout = move(layout);
    add_entry(move(entry));
}

ErrorOr<void> Layout::try_add_widget(Widget& widget)
{
    TRY(try_add_entry(Entry {
        .type = Entry::Type::Widget,
        .widget = widget,
    }));
    return {};
}

void Layout::add_widget(Widget& widget)
{
    MUST(try_add_widget(widget));
}

ErrorOr<void> Layout::try_insert_widget_before(Widget& widget, Widget& before_widget)
{
    Entry entry;
    entry.type = Entry::Type::Widget;
    entry.widget = widget;
    TRY(m_entries.try_insert_before_matching(move(entry), [&](auto& existing_entry) {
        return existing_entry.type == Entry::Type::Widget && existing_entry.widget.ptr() == &before_widget;
    }));
    if (m_owner)
        m_owner->notify_layout_changed({});
    return {};
}

void Layout::insert_widget_before(Widget& widget, Widget& before_widget)
{
    MUST(try_insert_widget_before(widget, before_widget));
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
