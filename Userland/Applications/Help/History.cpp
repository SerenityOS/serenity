/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "History.h"

void History::push(StringView history_item)
{
    if (!m_items.is_empty() && m_items[m_current_history_item] == history_item)
        return;

    m_items.shrink(m_current_history_item + 1);
    m_items.append(history_item);
    m_current_history_item++;
}

ByteString History::current()
{
    if (m_current_history_item == -1)
        return {};
    return m_items[m_current_history_item];
}

void History::go_back()
{
    VERIFY(can_go_back());
    m_current_history_item--;
}

void History::go_forward()
{
    VERIFY(can_go_forward());
    m_current_history_item++;
}

void History::clear()
{
    m_items = {};
    m_current_history_item = -1;
}
