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

void GLayout::add_layout(OwnPtr<GLayout>&& layout)
{
    Entry entry;
    entry.layout = move(layout);
    m_entries.append(move(entry));
    if (m_owner)
        m_owner->notify_layout_changed(Badge<GLayout>());
}

void GLayout::add_widget(GWidget& widget)
{
    Entry entry;
    entry.widget = widget.make_weak_ptr();
    m_entries.append(move(entry));
    if (m_owner)
        m_owner->notify_layout_changed(Badge<GLayout>());
}

void GLayout::remove_widget(GWidget& widget)
{
    m_entries.remove_first_matching([&] (auto& entry) {
        return entry.widget == &widget;
    });
    if (m_owner)
        m_owner->notify_layout_changed(Badge<GLayout>());
}

void GLayout::set_spacing(int spacing)
{
    if (m_spacing == spacing)
        return;
    m_spacing = spacing;
    if (m_owner)
        m_owner->notify_layout_changed(Badge<GLayout>());
}

void GLayout::set_margins(const GMargins& margins)
{
    if (m_margins == margins)
        return;
    m_margins = margins;
    if (m_owner)
        m_owner->notify_layout_changed(Badge<GLayout>());
}
