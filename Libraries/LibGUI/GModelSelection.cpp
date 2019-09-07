#include <LibGUI/GAbstractView.h>
#include <LibGUI/GModelSelection.h>

void GModelSelection::set(const GModelIndex& index)
{
    ASSERT(index.is_valid());
    if (m_indexes.size() == 1 && m_indexes.contains(index))
        return;
    m_indexes.clear();
    m_indexes.set(index);
    m_view.notify_selection_changed({});
}

void GModelSelection::add(const GModelIndex& index)
{
    ASSERT(index.is_valid());
    if (m_indexes.contains(index))
        return;
    m_indexes.set(index);
    m_view.notify_selection_changed({});
}

bool GModelSelection::remove(const GModelIndex& index)
{
    ASSERT(index.is_valid());
    if (!m_indexes.contains(index))
        return false;
    m_indexes.remove(index);
    m_view.notify_selection_changed({});
    return true;
}

void GModelSelection::clear()
{
    if (m_indexes.is_empty())
        return;
    m_indexes.clear();
    m_view.notify_selection_changed({});
}
