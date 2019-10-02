#include "History.h"

void History::push(const StringView& history_item)
{
    m_items.shrink(m_current_history_item + 1);
    m_items.append(history_item);
    m_current_history_item++;
}

String History::current()
{
    if (m_current_history_item == -1)
        return {};
    return m_items[m_current_history_item];
}

void History::go_back()
{
    ASSERT(can_go_back());
    m_current_history_item--;
}

void History::go_forward()
{
    ASSERT(can_go_forward());
    m_current_history_item++;
}

void History::clear()
{
    m_items = {};
    m_current_history_item = -1;
}
