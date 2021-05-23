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
    if (!m_items.is_empty() && m_items[m_current] == url)
        return;
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

void History::go_back(int steps)
{
    VERIFY(can_go_back(steps));
    m_current -= steps;
}

void History::go_forward(int steps)
{
    VERIFY(can_go_forward(steps));
    m_current += steps;
}

void History::clear()
{
    m_items = {};
    m_current = -1;
}

const Vector<URL> History::get_back_history()
{
    Vector<URL> back_history;
    for (int i = m_current - 1; i >= 0; i--) {
        back_history.append(m_items[i]);
    }
    return back_history;
}

const Vector<URL> History::get_forward_history()
{
    Vector<URL> forward_history;
    for (int i = m_current + 1; i < static_cast<int>(m_items.size()); i++) {
        forward_history.append(m_items[i]);
    }
    return forward_history;
}

}
