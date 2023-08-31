/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/History.h>

namespace WebView {

void History::dump() const
{
    dbgln("Dump {} items(s)", m_items.size());
    int i = 0;
    for (auto& item : m_items) {
        dbgln("[{}] {} '{}' {}", i, item.url, item.title, m_current == i ? '*' : ' ');
        ++i;
    }
}

Vector<History::URLTitlePair> History::get_all_history_entries()
{
    return m_items;
}

void History::push(const URL& url, DeprecatedString const& title)
{
    if (!m_items.is_empty() && m_items[m_current].url == url)
        return;
    m_items.shrink(m_current + 1);
    m_items.append(URLTitlePair {
        .url = url,
        .title = title,
    });
    m_current++;
}

void History::replace_current(const URL& url, DeprecatedString const& title)
{
    if (m_current == -1)
        return;

    m_items.remove(m_current);
    m_current--;
    push(url, title);
}

History::URLTitlePair History::current() const
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

void History::update_title(DeprecatedString const& title)
{
    if (m_current == -1)
        return;
    m_items[m_current].title = title;
}

Vector<StringView> const History::get_back_title_history()
{
    Vector<StringView> back_title_history;
    for (int i = m_current - 1; i >= 0; i--) {
        back_title_history.append(m_items[i].title);
    }
    return back_title_history;
}

Vector<StringView> const History::get_forward_title_history()
{
    Vector<StringView> forward_title_history;
    for (int i = m_current + 1; i < static_cast<int>(m_items.size()); i++) {
        forward_title_history.append(m_items[i].title);
    }
    return forward_title_history;
}

}
