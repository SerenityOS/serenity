/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "History.h"

namespace Browser {

void History::dump() const
{
    dbgln("Dump {} items(s)", m_items.size());
    int i = 0;
    for (auto& item : m_items) {
        dbgln("[{}] {} {}", i, item, m_current == i ? '*' : ' ');
        ++i;
    }
}

void History::push(const URL& url)
{
    m_items.shrink(m_current + 1);
    m_items.append(url);
    m_current++;
}

URL History::current() const
{
    if (m_current == -1)
        return {};
    return m_items[m_current];
}

void History::go_back()
{
    VERIFY(can_go_back());
    m_current--;
}

void History::go_forward()
{
    VERIFY(can_go_forward());
    m_current++;
}

void History::clear()
{
    m_items = {};
    m_current = -1;
}

}
