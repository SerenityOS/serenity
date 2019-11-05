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
